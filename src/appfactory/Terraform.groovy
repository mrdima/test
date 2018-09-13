package appfactory

import groovy.transform.Field

/**
 * Runs "terraform get" in the current directory
 */
def get() {
  terraformCommand("get")
}

/**
 * Run "terraform plan" with the provided prefix
 *
 * @param prefix String with desired prefix, optionally requested with the getRandomPrefix method
 */
def plan(String prefix) {
  terraformCommand("plan", "${prefix}")
}

/**
 * Run "terraform apply" with the provided prefix. Deploys an environment to
 * the provider currently set in main.tf, in the current directory.
 * Automatically retries to bypass flakiness due to an Azure SDK bug (and VPC in general).
 *
 * @param prefix String with desired prefix, optionally requested with the getRandomPrefix method
 */
def apply(String prefix) {
  retry(retries) {
    try {
      terraformCommand("apply", "${prefix}")
    } catch(err) {
      echo "Apply failed..."
      handleApplyError("${prefix}", err.toString())
    }
  }
}

/**
 * Run "terraform destroy" with the provided prefix. Destroys based on the default
 * terraform.tfstate in the current directory.
 * Automatically retries to bypass flakiness due to an Azure SDK bug (and VPC in general).
 *
 * @param prefix String with desired prefix, optionally requested with the getRandomPrefix method
 */
def destroy(String prefix) {
  retry(retries) {
    terraformCommand("destroy", "${prefix}")
  }
}

/**
 * Run "terraform destroy" with the provided prefix. Destroys based on the default
 * terraform.tfstate in the current directory.
 * Automatically retries to bypass flakiness due to an Azure SDK bug (and VPC in general).
 *
 * @param environment Map containing prefix. The Map return from createEnvironment
 * can be directly passed into this method.
 */
def destroy(Map environment) {
  retry(retries) {
    terraformCommand("destroy", "${environment.prefix}")
  }
}

/**
 * Writes credentials needed by terraform to the terraform.tfvars in the current directory,
 * or whichever is currently set as checkoutPath.
 */
def writeCredentials() {
  withCredentials([ string(credentialsId: 'tfvars.azure', variable: 'AZURE'),
                    string(credentialsId: 'tfvars.vpc', variable: 'VPC')]) {
    writeFile(file: "secrets-azure.tfvars", text: Utils.base64Decode("${env.AZURE}"))
    writeFile(file: "secrets-vpc.tfvars", text: Utils.base64Decode("${env.VPC}"))
  }
  sh "cat secrets-azure.tfvars secrets-vpc.tfvars >> ${checkoutPath}/terraform.tfvars"
}

/**
 * Writes the SSH Key saved in a Jenkins secret variable to a file
 *
 * @param path String with the desired path to save the SSH Key, such as "/home/jenkins/id_rsa"
 */
def writeSshKey(String path) {
  withCredentials([ string(credentialsId: 'ssh-key', variable: 'SSH') ]) {
    writeFile(file: "${path}", text: Utils.base64Decode("${env.SSH}"))
  }
}

/**
 * Generates a random prefix, starting with "t" and followed by 3 random characters. Used to
 * generate a test environment.
 *
 * @return String
 */
String getRandomPrefix() {
  return "t" + Utils.randomString(3)
}

/**
 * Creates a new test environment on the desired cloud provider. In case a terraform deployment isn't part
 * of the actual test.
 *
 * @param cloud String (optional) with the desired cloud provider. Defaults to "azure".
 * @param branch String (optional) with the desired branch. Defaults to "master".
 * @param path String (optional) with the relative checkout directory for terraform. Defaults to "appfactory-terraform"
 * @return Map containing keys "prefix". To be passed to the destroy method.
 */
Map createEnvironment(String cloud = "azure", String branch = "master", String path = "appfactory-terraform") {
  checkoutPath = "${path}"

  checkoutTerraform("${cloud}", "${branch}")

  String prefix = getRandomPrefix()

  writeCredentials()

  get()
  apply("${prefix}")

  // Returning prefix and token to be able to destroy the environment later
  return [prefix: "${prefix}"]
}

def setCheckoutPath(String path) {
  checkoutPath = path
}

/**
 * Private methods, not for consumption
 *
 * NOTE: Groovy don't care about your privacy! This is merely for reference.
 */

@Field private String args = "-no-color" // Improves readability in Jenkins console
@Field private String repo = "ssh://git@bitbucket.kpnis.nl:7999/appf/appfactory-terraform.git"
@Field private String checkoutPath = "." // Default checkout path unless overridden by createEnvironment
@Field private Integer retries = 3 // "retry(3)" in Jenkins means try three times, not four
@Field private Integer currentTries = 1 // Used in createEnvironment to automatically destroy if it fails after the amount of retries

private def handleApplyError(String prefix, String err) {
  if (currentTries >= retries) {
    echo "Maximum attempts exceeded, destroying..."

    destroy(prefix)

    throw new Exception("Destroyed environment after ${currentTries} tries. Reason: ${err}")
  }

  echo "Currently on attempt: ${currentTries}"
  currentTries++

  throw new Exception("Terraform failed. Reason: ${err}")
}

private def terraformCommand(String command) {
  sh "cd ${checkoutPath} && terraform ${command} ${args}"
}

private def terraformCommand(String command, String prefix) {
  if (command == "destroy") {
    // Forcing so terraform doesn't hang on "are you sure?"
    args = args + " -force"
  }

  terraformCommand(command + " -var customer-prefix=${prefix}")
}

private def checkoutTerraform(String cloud, String branch) {
  checkout([
    $class: 'GitSCM',
    branches: [[name: "*/${branch}"]],
    doGenerateSubmoduleConfigurations: false,
    extensions: [[$class: 'RelativeTargetDirectory', relativeTargetDir: "${checkoutPath}"]],
    submoduleCfg: [],
    userRemoteConfigs: [[credentialsId: 'git-key', url: "${repo}"]]
  ])

  sh "cp ${checkoutPath}/main.${cloud} ${checkoutPath}/main.tf"
}
