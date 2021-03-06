
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

//    Checkout current project
      checkout scm

      gitCommit = sh(returnStdout: true, script: 'git rev-parse --short HEAD').trim()

      stage('Login to nexus') {
        ansiColor('xterm'){
          withCredentials([usernamePassword(credentialsId: 'nexus', passwordVariable: 'PASS', usernameVariable: 'USER')]) {
            sh "docker login registry.platform.svc.appfactory.local:5000 -u ${env.USER} -p ${env.PASS}"
          }
        }
      }

//    Checkout i24 project

      String gitKey = '/home/jenkins/git_id_rsa'
      withCredentials([
        sshUserPrivateKey(credentialsId: '24i-agalani', keyFileVariable: 'GIT_KEY')
      ]) {
        String gitKeyContents = sh(script: "cat ${GIT_KEY}", returnStdout: true).trim()
        writeFile(file: "${gitKey}", text: gitKeyContents)
        sh """
        chmod 600 /home/jenkins/git_id_rsa
        eval `ssh-agent -s`
        ssh-add ${gitKey}
        mkdir ~/.ssh
        ssh-keyscan bitbucket.org >> ~/.ssh/known_hosts
        git clone git@bitbucket.org:24imedia/kpn-lg-pilot-build.git backend/24
        git clone git@bitbucket.org:24imedia/kpn-lg-pilot-build.git frontend/24
        """
      }

      stage('Build Docker images') {
        ansiColor('xterm'){
          sh """
          cd backend
          docker build -t registry.platform.svc.appfactory.local:5000/smarttv-backend:${gitCommit} .
          cd ../frontend
          docker build -t registry.platform.svc.appfactory.local:5000/smarttv-frontend:${gitCommit} .
          """
        }
      }

      stage('Push Docker image') {
        ansiColor('xterm'){
          withCredentials([usernamePassword(credentialsId: 'nexus', passwordVariable: 'PASS', usernameVariable: 'USER')]) {
            sh """
            docker push registry.platform.svc.appfactory.local:5000/smarttv-backend:${gitCommit}
            docker push registry.platform.svc.appfactory.local:5000/smarttv-frontend:${gitCommit}
            """
          }
        }
      }
   }
}
