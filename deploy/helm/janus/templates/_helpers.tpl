{{- define "janus.name" -}}
{{- default .Chart.Name .Values.nameOverride | trunc 63 | trimSuffix "-" -}}
{{- end -}}

{{- define "janus.fullname" -}}
{{- if .Values.fullnameOverride -}}
{{- .Values.fullnameOverride | trunc 63 | trimSuffix "-" -}}
{{- else -}}
{{- printf "%s-%s" .Release.Name (include "janus.name" .) | trunc 63 | trimSuffix "-" -}}
{{- end -}}
{{- end -}}

{{- define "janus.namespace" -}}
{{- if .Values.namespaceOverride -}}
{{- .Values.namespaceOverride -}}
{{- else -}}
{{- .Release.Namespace -}}
{{- end -}}
{{- end -}}

{{- define "janus.labels" -}}
helm.sh/chart: {{ .Chart.Name }}-{{ .Chart.Version | replace "+" "_" }}
app.kubernetes.io/name: {{ include "janus.name" . }}
app.kubernetes.io/instance: {{ .Release.Name }}
app.kubernetes.io/version: {{ .Chart.AppVersion | quote }}
app.kubernetes.io/managed-by: {{ .Release.Service }}
{{- end -}}

{{- define "janus.selectorLabels" -}}
app.kubernetes.io/name: {{ include "janus.name" . }}
app.kubernetes.io/instance: {{ .Release.Name }}
{{- end -}}

{{- define "janus.backendServiceName" -}}
{{ include "janus.fullname" . }}-backend
{{- end -}}
{{- define "janus.keycloakServiceName" -}}
{{ include "janus.fullname" . }}-keycloak
{{- end -}}
{{- define "janus.grafanaServiceName" -}}
{{ include "janus.fullname" . }}-grafana
{{- end -}}
{{- define "janus.postgresServiceName" -}}
{{ include "janus.fullname" . }}-postgres
{{- end -}}
{{- define "janus.postgresKeycloakServiceName" -}}
{{ include "janus.fullname" . }}-postgres-keycloak
{{- end -}}
