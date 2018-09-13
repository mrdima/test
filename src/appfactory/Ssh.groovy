package appfactory

import groovy.transform.Field

/******************
 * Global private variables
 */

@Field private sshKeyPath = ""
@Field private masterhostname = ""
@Field private jumpHost = ""

def writeSshKey(String id_rsa = "id_rsa") {

    sshKeyPath = id_rsa
    withCredentials([ string(credentialsId: 'ssh-key', variable: 'SSH') ]) {
         writeFile(file: "${sshKeyPath}", text: Utils.base64Decode("${env.SSH}"))
    }

    // copy to temporary file and change mod (needed for ssh-add). writeFile is executed under a different
    // user as sh command.
    sh "cp ${sshKeyPath} ${sshKeyPath}_new && chmod 400 ${sshKeyPath}_new && rm ${sshKeyPath} && mv ${sshKeyPath}_new ${sshKeyPath}"
}

String executeCommandOnHost(String hostName, String command) {

    String output = sh (
        script: "ssh -i ${sshKeyPath} -o UserKnownHostsFile=/dev/null -o StrictHostKeyChecking=no core@${hostName} ${command}",
        returnStdout: true
    ).trim()

    return output;
}

def setJumpHost(String jumpHostToSet) {
  jumpHost = jumpHostToSet
}

def findMasterHostName() {

    jumphostname = executeCommandOnHost(jumpHost, "hostname")


    switch (jumphostname) {
        case "jumphost0":
            // we are on azure
            masterhostname = "k8smaster0"
            break
        case ~/.*jump0/:
            // we are on CloudNL VMWare
            def vpcprefix = jumphostname.replaceFirst(/jump0/, "");
            masterhostname = vpcprefix + "master0"
            break
        default:
            throw new Exception("could not find MasterHostName using jumphost: " + jumphostname);
    }
}

int executeCommmandWithTunnelOnHst(String jumpHost, String remoteHost, String localPort, String remotePort, String endPoint, String command)  {
    int output = sh (
        script: "eval `ssh-agent` > /dev/null 2>&1 && ssh-add " + sshKeyPath + " > /dev/null 2>&1 && set +e; ssh -fN -L ${localPort}:${endPoint}:${remotePort} -o UserKnownHostsFile=/dev/null -o StrictHostKeyChecking=no -o ProxyCommand='ssh -o UserKnownHostsFile=/dev/null -o StrictHostKeyChecking=no core@" + jumpHost + " ncat " + remoteHost + " 22' core@${remoteHost}" + " > /dev/null 2>&1 && ${command}",
        returnStatus: true
    )
    return output
}

int executeCommandWithTunnel(String localPort, String remotePort, String endPoint, String command) {
    int output =  executeCommmandWithTunnelOnHst(jumpHost, masterhostname, localPort, remotePort, endPoint, command)
    return output
}
