package versioning

import groovy.transform.CompileStatic
import groovy.transform.TypeChecked
import org.eclipse.jgit.api.Git
import org.eclipse.jgit.lib.ObjectId
import org.eclipse.jgit.lib.Repository
import org.eclipse.jgit.revwalk.RevCommit
import org.eclipse.jgit.revwalk.RevWalk
import org.eclipse.jgit.storage.file.FileRepositoryBuilder
import org.joda.time.DateTime
import org.joda.time.DateTimeZone

@CompileStatic @TypeChecked
class GitVersioner {

	static GitInfo versionForDir(String dir) {
		versionForDir(new File(dir))
	}
	static int getCountCommit(Repository repo) {
		Iterable<RevCommit> commits = Git.wrap(repo).log().call()
		int count = 0;
		commits.each {
			count++;
		}

		return count;
	}
	// return last commit excluding merge commit 
	static RevCommit parseCommitLast(Repository repo) {
		Iterable<RevCommit> commits = Git.wrap(repo).log().call()
		for (RevCommit b : commits) {
			if (b.getParents().length > 1) { // it's merge commit ignore it
				continue;
			}
			return b;
		}

		return null;
	}
	static GitInfo versionForDir(File dir) {
		FileRepositoryBuilder builder = new FileRepositoryBuilder()
		Repository repo = builder.setWorkTree(dir)
			.findGitDir()
			.build()

		ObjectId head = repo.resolve('HEAD')
		if (!head) {
			return null
		}

		def commit = new RevWalk(repo).parseCommit(head)
		def commitLast = parseCommitLast(repo)
		int commitCount = getCountCommit(repo)

		def branch = repo.getBranch()
		def commitDate = new DateTime(1000L * commit.commitTime, DateTimeZone.UTC)

		if (!commit) {
			throw new RuntimeException("Can't find last commit.")
		}

		String tag = repo.tags.find { kv -> kv.value.objectId == commit.id }?.key
		String headCommitId = commit.getId().abbreviate(7).name();
		String authorCommit = commitLast.getAuthorIdent().getName();

		return new GitInfo(
			lastCommitDate: commitDate,
			branch: branch,
			tag: tag,
			countCommit: commitCount,
			commitID: headCommitId,
			authorCommit: authorCommit
		)
	}
}
