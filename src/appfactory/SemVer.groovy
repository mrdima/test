package appfactory

String getNewVersion() {
  setMergeCommitMessage()
  // Adding version to new array to make sure values are integers
  def newVersion = []
  def lastVersion = getLastVersion()

  lastVersion.each {
    newVersion.add(it.toInteger())
  }

  return parseVersion(newVersion)
}

def tagVersion(version) {
  sshagent(['git-key']) {
    sh """
    git config --global user.email "jenkins@kpnappfactory.com"
    git config --global user.name "Jenkins"
    git tag -a -m \'${mergeCommit}\' ${version} && git push origin : ${version}
    """
  }
}

private def mergeCommit

private def setMergeCommitMessage() {
  def gitCommit = sh(returnStdout: true, script: 'git rev-parse --short HEAD').trim()

  mergeCommit = sh(returnStdout: true, script: "git log -n 1 --pretty=format:%s ${gitCommit}").trim()
}

private def getLastVersion() {
  try {
    return sh(returnStdout: true, script: 'git describe --abbrev=0 --tags').trim().split('\\.')
  } catch(err) {
    echo "No tags available"
    return [0,0,0]
  }
}

private String parseVersion(newVersion) {
  if (mergeCommit.contains(' from breaking/')) {
    newVersion[0]++
    newVersion[1] = 0
    newVersion[2] = 0
  } else if (mergeCommit.contains(' from feature/')) {
    newVersion[1]++
    newVersion[2] = 0
  } else if (mergeCommit.contains(' from fix/')) {
    newVersion[2]++
  } else {
    throw new Exception("Unable to parse version from commit message: ${mergeCommit}")
  }

  return newVersion.join('.')
}
