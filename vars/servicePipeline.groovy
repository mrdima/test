import appfactory.Build
import appfactory.Terraform
import static appfactory.Utils.*

def call(String svcName, String gitCommit, String pullRequestId, boolean deployExamplePipeline = false, Closure body) {
  def build = new Build()
  build.clearTemplateNames()

  podTemplate(name: "${svcName}", inheritFrom: 'jnlp', label: "${svcName}", containers: [
    containerTemplate(name: 'appfctl',
                      image: 'kpnappfactory/appfctl:0.12.1',
                      ttyEnabled: true, command: 'cat',
                      resourceLimitCpu: '250m',
                      resourceRequestCpu: '250m',
                      resourceRequestMemory: '512Mi',
                      resourceLimitMemory: '1024Mi',
                      alwaysPullImage: false)
    ])
  {
    node("${svcName}") {
      checkout scm

      Integer retries = 3
      def sshKey = "/home/jenkins/id_rsa"
      def gitKey = "/home/jenkins/git_id_rsa"
      def terraform = new Terraform()
      String customer = terraform.getRandomPrefix()

      if (gitCommit == null) {
        imageVersionVars = ""
      }

      container('appfctl') {
        withCredentials([
          string(credentialsId: 'tfvars.azure', variable: 'AZURE'),
          string(credentialsId: 'tfvars.vpc', variable: 'VPC'),
          string(credentialsId: 'ssh-key', variable: 'SSH_KEY'),
          string(credentialsId: 'vault-pass', variable: 'VAULT_PASS'),
          sshUserPrivateKey(credentialsId: 'git-key', keyFileVariable: 'GIT_KEY')
        ]) {
          String vaultPassword = base64Decode("${VAULT_PASS}")
          String gitKeyContents = sh(script: "cat ${GIT_KEY}", returnStdout: true).trim()
          writeFile(file: "${sshKey}", text: base64Decode("${SSH_KEY}"))
          writeFile(file: "${gitKey}", text: gitKeyContents)
          writeFile(file: "secrets-azure.tfvars", text: base64Decode("${AZURE}"))
          writeFile(file: "secrets-vpc.tfvars", text: base64Decode("${VPC}"))
          writeFile(file: "config.yml", text: '---')
          sh """
          set +x
          echo 'log-level: debug' >> config.yml
          echo 'ssh-user: core' >> config.yml
          echo 'ssh-private-key-file: ${sshKey}' >> config.yml
          echo 'local: true' >> config.yml
          echo 'vault-password: ${base64Decode("${VAULT_PASS}")}' >> config.yml
          """
        }

        try {
          stage('Setup environment') {

            // Configures a virtualenv and checks if any packages in requirements.txt need to be updated
            // Make sure to add updates to the bootstrap image (runtime) or tf-builder image (buildtime)
            // as well after a PR is merged, to decrease CI time.
            sh """
            eval `ssh-agent -s`
            ssh-add ${sshKey}
            ssh-add ${gitKey}
            appfctl --config-path . -c ${customer} --environment test setup --skip-terraform-settings
            """
            retry (retries) {
              sh """
              eval `ssh-agent -s`
              ssh-add ${sshKey}
              ssh-add ${gitKey}
              appfctl --config-path . -c ${customer} --environment test apply --non-interactive \
                --terraform-settings-path . \
                --ansible-extra-var enable_all_services=False \
                --ansible-extra-var deploy_example_pipeline=${deployExamplePipeline} \
                --ansible-extra-var \"{services: {${svcName}: {enabled: True, branch: refs/pull-requests/${pullRequestId}/merge}}}\" \
                --ansible-extra-var \"{${svcName}: {version: ${gitCommit}}}\"
              """
            }
          }
        } catch (Exception err) {
        echo "Caught: ${err}"
        throw err
        } finally {
          stage('Cleanup environment') {
            container('appfctl') {
              retry(retries) {
                sh "appfctl --config-path . -c ${customer} --environment test delete --terraform-settings-path . --non-interactive"
              }
            }
          }
        }
      }
    }
  }
}