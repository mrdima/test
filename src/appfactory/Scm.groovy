package appfactory;

static def getBranchName(scm) {
  return scm.branches[0].name
}
