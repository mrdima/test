
def nodeLabel = "ss-build-${UUID.randomUUID().toString()}"
podTemplate(name: "ss-build", serviceAccount: 'serverspec-sa', label: nodeLabel, containers: [
  containerTemplate(
      name: 'jnlp',
      image: 'kpnappfactory/jnlp-slave:3.0.3',
      args: '${computer.jnlpmac} ${computer.name}',
      envVars: [
        containerEnvVar(key: 'JAVA_OPTS', value: '-Xmx512m')
      ],
      ttyEnabled: false,
      resourceRequestCpu: '250m',
      resourceLimitCpu: '500m',
      resourceRequestMemory: '512Mi',
      resourceLimitMemory: '768Mi',
      alwaysPullImage: false)
 ])

{
    node(nodeLabel) {
      checkout scm

      gitCommit = sh(returnStdout: true, script: 'git rev-parse --short HEAD').trim()
      workDir = sh(returnStdout: true, script: 'pwd').trim()

      stage('Build Docker image') {
        ansiColor('xterm'){
          sh "echo hoi"
        }
      }
}
