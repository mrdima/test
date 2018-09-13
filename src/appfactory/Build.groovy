package appfactory
import org.csanchez.jenkins.plugins.kubernetes.pipeline.PodTemplateAction

def getBuildCause() {
  String buildCause = currentBuild.rawBuild.getCause(hudson.model.Cause).toString()

  return buildCause
}

/**
 * This is a workaround for a podTemplate inheritance bug, allowing us to use
 * multiple podTemplates in one pipeline.
 * See: https://issues.jenkins-ci.org/browse/JENKINS-42184
 */
def clearTemplateNames() {
  def build = currentBuild.rawBuild
  def action = build.getAction(PodTemplateAction.class)
  if(action) {
    if (action.hasProperty('names')) {
      action.names?.clear()
    }

    if (action.hasProperty('stack')) {
      action.stack?.clear()
    }
  }
}
