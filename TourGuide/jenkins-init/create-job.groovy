/* create job tourguide-ci which will read Jenkinsfile */

import jenkins.model.*
import hudson.plugins.git.*
import org.jenkinsci.plugins.workflow.job.*
import org.jenkinsci.plugins.workflow.cps.*
import hudson.model.*

def jenkins = Jenkins.instance
def jobName = "tourguide-ci"
def repositoryUrl = "https://github.com/Lulippe-Hiboude/JavaPathENProject8"
def branchName = "*/master"
def scriptsPath = "Jenkinsfile"

if (jenkins.getItem(jobName) == null) {
    println("Creating Jenkins pipeline jon '${jobName}'...")
    def scm = new GitSCM(repositoryUrl)
    scm.branches = [new BranchSpec(branchName)]

    def definition = new CpsScmFlowDefinition(scm, scriptsPath)
    definition.setLightweight(true)

    WorkflowJob job = jenkins.createProject(WorkflowJob, jobName)
    job.setDefinition(definition)
    job.save()

    job.scheduleBuild2(0)

    println("Job '${jobName}' created and build scheduled.")

} else {
    println "Job '${jobName}' already exists."
}