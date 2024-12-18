package es.nivel36.janus.web.core.component;

import es.nivel36.janus.service.schedule.TimeRange;
import es.nivel36.janus.service.workshift.WorkShift;
import jakarta.faces.component.FacesComponent;
import jakarta.faces.component.UIComponentBase;

/**
 * JSF component for handling work shifts in a custom UI. This component provides
 * support for managing {@link WorkShift} and {@link TimeRange} objects within a JSF page.
 *
 * The component is registered with the tag name "workShift" in the namespace
 * "http://nivel36.es/jsf/components". It can be used to represent and manipulate
 * work shifts in a user interface.
 *
 * <h3>Attributes:</h3>
 * <ul>
 *   <li><strong>workShift:</strong> The {@link WorkShift} object associated with this component.</li>
 *   <li><strong>timeRange:</strong> The {@link TimeRange} representing the schedule of the work shift.</li>
 * </ul>
 *
 * Example usage:
 * <pre>{@code
 * <n:workShift workShift="#{bean.workShift}" timeRange="#{bean.timeRange}" />
 * }</pre>
 *
 * <h3>Component Properties:</h3>
 * <ul>
 *   <li><strong>Renderer Type:</strong> The renderer type is set to "es.nivel36.WorkShiftRenderer".</li>
 *   <li><strong>Component Family:</strong> The component belongs to the "es.nivel36.components" family.</li>
 * </ul>
 *
 * Note: Ensure that the required renderer ("es.nivel36.janus.web.core.component.WorkShiftRenderer") is implemented
 * and registered in your JSF application to properly render this component.
 */
@FacesComponent(value = "es.nivel36.WorkShiftComponent", createTag = true, namespace = "http://nivel36.es/jsf/components", tagName = "workShift")
public class WorkShiftComponent extends UIComponentBase {

    /**
     * Retrieves the associated {@link WorkShift} object.
     *
     * @return the {@link WorkShift} object, or null if not set
     */
    public WorkShift getWorkShift() {
        return (WorkShift) this.getStateHelper().eval("workShift");
    }

    /**
     * Retrieves the {@link TimeRange} object representing the schedule of the work shift.
     *
     * @return the {@link TimeRange}, or null if not set
     */
    public TimeRange getTimeRange() {
        return (TimeRange) this.getStateHelper().eval("timeRange");
    }

    /**
     * Sets the {@link TimeRange} for this component.
     *
     * @param timeRange the {@link TimeRange} to set
     */
    public void setTimeRange(final TimeRange timeRange) {
        this.getStateHelper().put("timeRange", timeRange);
    }

    /**
     * Sets the {@link WorkShift} for this component.
     *
     * @param workShift the {@link WorkShift} to set
     */
    public void setWorkShif(final WorkShift workShift) {
        this.getStateHelper().put("workShift", workShift);
    }

    /**
     * Specifies the renderer type for this component.
     *
     * @return the renderer type, "es.nivel36.WorkShiftRenderer"
     */
    @Override
    public String getRendererType() {
        return "es.nivel36.WorkShiftRenderer";
    }

    /**
     * Specifies the component family for this component.
     *
     * @return the component family, "es.nivel36.components"
     */
    @Override
    public String getFamily() {
        return "es.nivel36.components";
    }
}
