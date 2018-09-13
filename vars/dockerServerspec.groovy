import appfactory.Build
import appfactory.SemVer
import static appfactory.Scm.*

def call(String imageName, String tagName, Closure body) {
  def semVer = new SemVer()
  def build = new Build()
  build.clearTemplateNames()

  podTemplate(name: "ss-build", inheritFrom: 'jnlp', label: "ss-build", containers: [
    containerTemplate(name: 'serverspec',
                      image: 'kpnappfactory/serverspec:1.4.5',
                      ttyEnabled: true,
                      command: 'cat',
                      envVars: [
                        containerEnvVar(key: "GENERATE_REPORTS", value: "true"),
                        containerEnvVar(key: "CI_REPORTS", value: "/usr/src/app/spec/${imageName}/reports"),
                        containerEnvVar(key: "KUBERNETES_NAMESPACE", value: "jenkins-slaves"),
                        containerEnvVar(key: "IMAGE_NAME", value: "${tagName}")
                      ],
                      resourceLimitCpu: '100m',
                      resourceRequestCpu: '50m',
                      resourceRequestMemory: '100Mi',
                      resourceLimitMemory: '200Mi',
                      alwaysPullImage: true)])
  {
    node("ss-build") {
      checkout scm

      body()

      gitCommit = sh(returnStdout: true, script: 'git rev-parse --short HEAD').trim()
      workDir = sh(returnStdout: true, script: 'pwd').trim()

      withCredentials([usernamePassword(credentialsId: 'dockerhub', passwordVariable: 'PASS', usernameVariable: 'USER')]) {
          sh "docker login -u ${env.USER} -p ${env.PASS}"
      }

      stage('Build Docker image') {
        sh "make build IMAGE_NAME=${imageName} IMAGE_VERSION=${gitCommit}"
      }

      }
    }
  }