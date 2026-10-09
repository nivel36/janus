/*
 * Copyright 2026 Abel Ferrer Jiménez
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */
package es.nivel36.janus.api;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.lang.reflect.AnnotatedElement;
import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.lang.reflect.RecordComponent;
import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.springframework.core.annotation.AnnotatedElementUtils;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;
import org.springframework.core.type.classreading.CachingMetadataReaderFactory;
import org.springframework.data.domain.Pageable;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ValueConstants;
import org.yaml.snakeyaml.Yaml;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

class OpenApiContractTest {

	private static final String API_PREFIX = "/api/v1";
	private static final String RESOURCE_PATTERN = "classpath*:es/nivel36/janus/api/v1/**/*Resource.class";
	private static final Set<String> HTTP_METHODS = Set
			.of("get", "put", "post", "delete", "patch", "head", "options", "trace");

	@Test
	void openApiOperationsMatchEveryRestResourceBidirectionally() throws Exception {
		final Map<String, DocumentedOperation> documented = documentedOperations(contract());
		final Map<String, ImplementedOperation> implemented = implementedOperations();

		assertThat(documented.keySet()).as("documented operations must equal canonical implemented operations")
				.containsExactlyInAnyOrderElementsOf(implemented.keySet());
	}

	@Test
	void openApiDocumentsParametersBodiesValidationsAndSuccessResponses() throws Exception {
		final Map<String, DocumentedOperation> documented = documentedOperations(contract());

		for (final Map.Entry<String, ImplementedOperation> entry : implementedOperations().entrySet()) {
			final String operationKey = entry.getKey();
			final ImplementedOperation implementation = entry.getValue();
			final DocumentedOperation documentation = documented.get(operationKey);
			assertThat(documentation).as("documentation for %s", operationKey).isNotNull();
			assertParameters(operationKey, implementation, documentation);
			assertRequestBody(operationKey, implementation.method(), documentation.operation());
			assertSuccessResponse(operationKey, implementation.method(), documentation.operation());
			if (implementation.compatibilityMapping()) {
				assertThat(documentation.operation().get("deprecated"))
						.as("compatibility mapping %s must remain documented as deprecated", operationKey)
						.isEqualTo(true);
			}
		}
	}

	@Test
	@SuppressWarnings("unchecked")

	void timeLogResponsesRequireStableEmployeeNumber() throws Exception {
		final Map<String, Map<String, Object>> schemas = (Map<String, Map<String, Object>>) ((Map<String, Object>) contract()
				.get("components")).get("schemas");

		for (final String schemaName : List.of("ClockOutWithoutClockInEventResponse")) {
			final Map<String, Object> schema = schemas.get(schemaName);
			final List<String> required = (List<String>) schema.get("required");
			final Map<String, Map<String, Object>> properties = (Map<String, Map<String, Object>>) schema
					.get("properties");
			assertThat(required).as("required properties for %s", schemaName).contains("employeeNumber");
			assertThat(properties.get("employeeNumber")).containsEntry("type", "string");
			assertThat(properties).doesNotContainKey("employeeEmail");
		}
	}

	@Test
	@SuppressWarnings("unchecked")
	void timeLogResponseDocumentsEmployeeAndWorksiteDetails() throws Exception {
		final Map<String, Object> spec = contract();
		final Map<String, Object> components = (Map<String, Object>) spec.get("components");
		final Map<String, Map<String, Object>> schemas = (Map<String, Map<String, Object>>) components.get("schemas");
		final Map<String, Object> response = schemas.get("TimeLogResponse");
		final List<String> required = (List<String>) response.get("required");
		final Map<String, Map<String, Object>> properties = (Map<String, Map<String, Object>>) response
				.get("properties");
		assertThat(required).contains("employee", "worksite");
		assertThat(properties).doesNotContainKeys("employeeNumber", "worksiteCode", "worksiteZoneId");
		assertThat(properties.get("employee")).containsEntry("$ref", "#/components/schemas/TimeLogEmployeeResponse");
		assertThat(properties.get("worksite")).containsEntry("$ref", "#/components/schemas/TimeLogWorksiteResponse");
		assertThat((List<String>) schemas.get("TimeLogEmployeeResponse").get("required"))
				.containsExactly("number", "fullName");
		assertThat((List<String>) schemas.get("TimeLogWorksiteResponse").get("required"))
				.containsExactly("code", "name", "zoneId");
	}

	@Test
	@SuppressWarnings("unchecked")
	void openApiDoesNotPublishRetiredClockOutEventRequestSchemas() throws Exception {
		final Map<String, Map<String, Object>> schemas = (Map<String, Map<String, Object>>) ((Map<String, Object>) contract()
				.get("components")).get("schemas");

		assertThat(schemas).containsKey("TransitionClockOutWithoutClockInEventRequest").doesNotContainKeys(
				"ResolveClockOutWithoutClockInEventRequest",
				"InvalidateClockOutWithoutClockInEventRequest");
	}

	@Test
	@SuppressWarnings("unchecked")
	void applicationSettingsDocumentsCanonicalFieldsValidationsAndErrors() throws Exception {
		final Map<String, Object> spec = contract();
		final Map<String, Map<String, Object>> schemas = (Map<String, Map<String, Object>>) ((Map<String, Object>) spec
				.get("components")).get("schemas");
		final List<String> fields = List.of(
				"daysUntilLocked",
				"employeeWorksiteCreationAllowed",
				"worksiteChangeDuringShiftAllowed",
				"employeeManualTimeLogEntryAllowed",
				"defaultTimezone");
		for (final String schemaName : List.of("ApplicationSettingsResponse", "UpdateApplicationSettingsRequest")) {
			final Map<String, Object> schema = schemas.get(schemaName);
			assertThat((List<String>) schema.get("required")).containsExactlyInAnyOrderElementsOf(fields);
			final Map<String, Map<String, Object>> properties = (Map<String, Map<String, Object>>) schema
					.get("properties");
			assertThat(properties.keySet()).containsExactlyInAnyOrderElementsOf(fields);
			assertThat(properties.get("daysUntilLocked")).containsEntry("type", "integer").containsEntry("minimum", 0);
			for (final String field : fields.subList(1, 4)) {
				assertThat(properties.get(field)).containsEntry("type", "boolean");
			}
			assertThat(properties.get("defaultTimezone")).containsEntry("type", "string");
		}
		final Map<String, Object> request = schemas.get("UpdateApplicationSettingsRequest");
		final Map<String, Map<String, Object>> properties = (Map<String, Map<String, Object>>) request
				.get("properties");
		assertThat(properties.get("defaultTimezone")).containsEntry("minLength", 1);
		final Map<String, DocumentedOperation> operations = documentedOperations(spec);
		for (final String verb : List.of("GET", "PUT")) {
			final Map<String, Object> operation = operations.get(verb + " /application-settings").operation();
			assertThat((Map<String, Object>) operation.get("responses")).containsKeys("200", "401", "403", "500");
			assertThat((String) operation.get("description")).contains("provisioned JWT identity", "JANUS_ADMIN");
		}
		assertThat((Map<String, Object>) operations.get("PUT /application-settings").operation().get("responses"))
				.containsKey("400");
	}

	private static Map<String, Object> contract() throws Exception {
		try (InputStream stream = OpenApiContractTest.class.getResourceAsStream("/janus.yaml");
				InputStreamReader reader = new InputStreamReader(stream)) {
			return new Yaml().load(reader);
		}
	}

	@SuppressWarnings("unchecked")
	private static Map<String, DocumentedOperation> documentedOperations(final Map<String, Object> contract) {
		final LinkedHashMap<String, DocumentedOperation> result = new LinkedHashMap<String, DocumentedOperation>();
		final Map<String, Map<String, Object>> paths = (Map<String, Map<String, Object>>) contract.get("paths");
		paths.forEach((path, pathItem) -> pathItem.forEach((verb, value) -> {
			if (HTTP_METHODS.contains(verb)) {
				result.put(
						verb.toUpperCase(Locale.ROOT) + " " + normalize(path),
						new DocumentedOperation(pathItem, (Map<String, Object>) value));
			}
		}));
		return result;
	}

	private static Map<String, ImplementedOperation> implementedOperations() throws Exception {
		final LinkedHashMap<String, ImplementedOperation> result = new LinkedHashMap<String, ImplementedOperation>();
		for (final Class<?> resource : restResources()) {
			final RequestMapping baseMapping = AnnotatedElementUtils
					.findMergedAnnotation(resource, RequestMapping.class);
			assertThat(baseMapping.value()).as("mapping for %s", resource.getSimpleName()).isNotEmpty();
			final String[] basePaths = baseMapping.value();
			for (final Method method : resource.getDeclaredMethods()) {
				final RequestMapping mapping = AnnotatedElementUtils.findMergedAnnotation(method, RequestMapping.class);
				if (mapping == null)
					continue;
				final String[] methodPaths = mapping.value().length == 0 ? new String[] { "" } : mapping.value();
				for (int baseIndex = 0; baseIndex < basePaths.length; baseIndex++) {
					final String baseMappingPath = basePaths[baseIndex];
					final String basePath = baseMappingPath.substring(API_PREFIX.length());
					for (int methodIndex = 0; methodIndex < methodPaths.length; methodIndex++) {
						final String methodPath = methodPaths[methodIndex];
						for (final RequestMethod httpMethod : mapping.method()) {
							final String key = httpMethod.name() + " " + normalize(basePath + methodPath);
							result.putIfAbsent(
									key,
									new ImplementedOperation(
											resource,
											method,
											baseIndex > 0 || methodIndex > 0
													|| method.isAnnotationPresent(Deprecated.class)));
						}
					}
				}
			}
		}
		return result;
	}

	private static Set<Class<?>> restResources() throws Exception {
		final PathMatchingResourcePatternResolver resolver = new PathMatchingResourcePatternResolver();
		final CachingMetadataReaderFactory readers = new CachingMetadataReaderFactory(resolver);
		final LinkedHashSet<Class<?>> resources = new LinkedHashSet<Class<?>>();
		for (final Resource resource : resolver.getResources(RESOURCE_PATTERN)) {
			final String className = readers.getMetadataReader(resource).getClassMetadata().getClassName();
			final Class<?> type = Class.forName(className);
			if (type.isInterface() && AnnotatedElementUtils.hasAnnotation(type, RequestMapping.class))
				resources.add(type);
		}
		assertThat(resources).as("REST resource interfaces discovered below api/v1").isNotEmpty();
		return resources;
	}

	@SuppressWarnings("unchecked")
	private static void assertParameters(
			final String key,
			final ImplementedOperation implementation,
			final DocumentedOperation documentation) {
		final ArrayList<Map<String, Object>> documented = new ArrayList<Map<String, Object>>();
		documented.addAll((List<Map<String, Object>>) documentation.pathItem().getOrDefault("parameters", List.of()));
		documented.addAll((List<Map<String, Object>>) documentation.operation().getOrDefault("parameters", List.of()));

		for (final Parameter parameter : implementation.method().getParameters()) {
			final PathVariable pathVariable = parameter.getAnnotation(PathVariable.class);
			final RequestParam requestParam = parameter.getAnnotation(RequestParam.class);
			if (pathVariable != null) {
				assertParameter(
						key,
						documented,
						annotationName(pathVariable.value(), pathVariable.name(), parameter),
						"path",
						true,
						parameter.getType(),
						parameter);
			} else if (requestParam != null) {
				assertParameter(
						key,
						documented,
						annotationName(requestParam.value(), requestParam.name(), parameter),
						"query",
						requestParam.required() && ValueConstants.DEFAULT_NONE.equals(requestParam.defaultValue()),
						parameter.getType(),
						parameter);
			} else if (parameter.isAnnotationPresent(ModelAttribute.class)) {
				assertModelAttributeParameters(key, documented, parameter);
			} else if (parameter.getType() == Pageable.class) {
				assertSimpleParameter(key, documented, "page", "query", false, "integer");
				assertSimpleParameter(key, documented, "size", "query", false, "integer");
				assertSimpleParameter(key, documented, "sort", "query", false, "array");
			}
		}
	}

	private static void assertModelAttributeParameters(
			final String key,
			final List<Map<String, Object>> documented,
			final Parameter parameter) {
		assertThat(parameter.getType().isRecord()).as("model attribute for %s must be a record", key).isTrue();
		for (final RecordComponent component : parameter.getType().getRecordComponents()) {
			assertParameter(key, documented, component.getName(), "query", false, component.getType(), component);
		}
	}

	@SuppressWarnings("unchecked")
	private static void assertParameter(
			final String key,
			final List<Map<String, Object>> documented,
			final String name,
			final String location,
			final boolean required,
			final Class<?> implementationType,
			final AnnotatedElement implementation) {
		final Map<String, Object> parameter = findParameter(key, documented, name, location);
		if (required) {
			assertThat(parameter.get("required")).as("required flag for %s parameter %s", key, name).isEqualTo(true);
		} else {
			assertThat(parameter.get("required")).as("required flag for %s parameter %s", key, name).isIn(null, false);
		}
		final Map<String, Object> schema = resolveSchema((Map<String, Object>) parameter.get("schema"));
		assertThat(schema.get("type")).as("type for %s parameter %s", key, name)
				.isEqualTo(openApiType(implementationType));
		if (implementationType == Instant.class)
			assertThat(schema.get("format")).isEqualTo("date-time");
		if (implementationType == UUID.class)
			assertThat(schema.get("format")).isEqualTo("uuid");
		final Pattern pattern = mergedPattern(implementation);
		if (pattern != null)
			assertThat(schema.get("pattern")).as("validation for %s parameter %s", key, name)
					.isEqualTo(fullValuePattern(pattern.regexp()));
		final Size size = AnnotatedElementUtils.findMergedAnnotation(implementation, Size.class);
		if (size != null && implementationType == String.class) {
			assertThat(schema.get("maxLength")).as("maximum length for %s parameter %s", key, name)
					.isEqualTo(size.max());
		}
	}

	/** Resolves both direct constraints and reusable composed constraints. */
	private static Pattern mergedPattern(final AnnotatedElement element) {
		return AnnotatedElementUtils.findMergedAnnotation(element, Pattern.class);
	}

	@SuppressWarnings("unchecked")
	private static Map<String, Object> resolveSchema(final Map<String, Object> schema) {
		final String reference = (String) schema.get("$ref");
		if (reference == null)
			return schema;
		try {
			final Map<String, Map<String, Object>> schemas = (Map<String, Map<String, Object>>) ((Map<String, Object>) contract()
					.get("components")).get("schemas");
			return schemas.get(reference.substring(reference.lastIndexOf('/') + 1));
		} catch (final Exception exception) {
			throw new IllegalStateException("Could not resolve OpenAPI schema " + reference, exception);
		}
	}

	private static void assertSimpleParameter(
			final String key,
			final List<Map<String, Object>> parameters,
			final String name,
			final String location,
			final boolean required,
			final String type) {
		final Map<String, Object> synthetic = findParameter(key, parameters, name, location);
		assertThat(synthetic.get("required")).isIn(null, required);
		assertThat(schema(synthetic).get("type")).as("type for %s parameter %s", key, name).isEqualTo(type);
	}

	private static Map<String, Object> findParameter(
			final String key,
			final List<Map<String, Object>> parameters,
			final String name,
			final String location) {
		return parameters.stream().filter(p -> name.equals(p.get("name")) && location.equals(p.get("in"))).findFirst()
				.orElseThrow(() -> new AssertionError("Missing " + location + " parameter " + name + " for " + key));
	}

	@SuppressWarnings("unchecked")
	private static Map<String, Object> schema(final Map<String, Object> parameter) {
		return (Map<String, Object>) parameter.get("schema");
	}

	@SuppressWarnings("unchecked")
	private static void assertRequestBody(final String key, final Method method, final Map<String, Object> operation) {
		for (final Parameter parameter : method.getParameters()) {
			final RequestBody body = parameter.getAnnotation(RequestBody.class);
			if (body == null)
				continue;
			final Map<String, Object> documented = (Map<String, Object>) operation.get("requestBody");
			assertThat(documented).as("request body for %s", key).isNotNull();
			assertThat(documented.get("required")).isEqualTo(body.required());
			final Map<String, Object> content = (Map<String, Object>) documented.get("content");
			final Map<String, Object> json = (Map<String, Object>) content.get("application/json");
			assertThat(schema(json).get("$ref"))
					.isEqualTo("#/components/schemas/" + parameter.getType().getSimpleName());
			assertBodyPatterns(key, parameter.getType());
			if (parameter.isAnnotationPresent(Valid.class))
				assertThat(documented).containsKey("required");
		}
	}

	@SuppressWarnings("unchecked")
	private static void assertBodyPatterns(final String key, final Class<?> bodyType) {
		if (!bodyType.isRecord())
			return;
		final Map<String, Object> bodySchema;
		try {
			final Map<String, Map<String, Object>> schemas = (Map<String, Map<String, Object>>) ((Map<String, Object>) contract()
					.get("components")).get("schemas");
			bodySchema = schemas.get(bodyType.getSimpleName());
		} catch (final Exception exception) {
			throw new IllegalStateException(
					"Could not resolve OpenAPI body schema " + bodyType.getSimpleName(),
					exception);
		}
		final Map<String, Map<String, Object>> properties = (Map<String, Map<String, Object>>) bodySchema
				.get("properties");
		for (final RecordComponent component : bodyType.getRecordComponents()) {
			final Pattern pattern = mergedPattern(component.getAccessor());
			final Map<String, Object> property = resolveSchema(properties.get(component.getName()));
			if (pattern != null) {
				assertThat(property.get("pattern")).as("validation for %s body property %s", key, component.getName())
						.isEqualTo(fullValuePattern(pattern.regexp()));
			}
			final Size size = AnnotatedElementUtils.findMergedAnnotation(component.getAccessor(), Size.class);
			if (size != null) {
				assertThat(property.get("maxLength"))
						.as("maximum length for %s body property %s", key, component.getName()).isEqualTo(size.max());
			}
		}
	}

	@SuppressWarnings("unchecked")
	private static void assertSuccessResponse(
			final String key,
			final Method method,
			final Map<String, Object> operation) {
		final Map<String, Object> responses = (Map<String, Object>) operation.get("responses");
		assertThat(responses).as("responses for %s", key).containsKey(expectedSuccessCode(method));
	}

	private static String expectedSuccessCode(final Method method) {
		if (method.getName().startsWith("delete") || method.getName().startsWith("assign")
				|| method.getName().startsWith("remove"))
			return "204";
		if (method.getName().startsWith("createEmployee") || method.getName().startsWith("createWorksite")
				|| method.getName().startsWith("createSchedule") || method.getName().startsWith("createTimeLog")
				|| method.getName().startsWith("clockIn"))
			return "201";
		return "200";
	}

	private static String annotationName(final String value, final String name, final Parameter parameter) {
		if (!value.isBlank())
			return value;
		if (!name.isBlank())
			return name;
		return parameter.getName();
	}

	private static String openApiType(final Class<?> type) {
		if (type == int.class || type == long.class || Number.class.isAssignableFrom(type))
			return "integer";
		if (type == boolean.class || type == Boolean.class)
			return "boolean";
		return "string";
	}

	private static String fullValuePattern(final String pattern) {
		final String withStartAnchor = pattern.startsWith("^") ? pattern : "^" + pattern;
		return withStartAnchor.endsWith("$") ? withStartAnchor : withStartAnchor + "$";
	}

	private static String normalize(final String path) {
		return path.length() > 1 && path.endsWith("/") ? path.substring(0, path.length() - 1) : path;
	}

	private record ImplementedOperation(Class<?> resource, Method method, boolean compatibilityMapping) {
	}

	private record DocumentedOperation(Map<String, Object> pathItem, Map<String, Object> operation) {
	}
}
