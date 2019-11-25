package io.scalecube.trace.git;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.attribute.PosixFilePermission;
import java.nio.file.attribute.PosixFilePermissions;
import java.util.HashSet;
import java.util.Set;
import org.eclipse.jgit.api.CloneCommand;
import org.eclipse.jgit.api.CreateBranchCommand.SetupUpstreamMode;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.ResetCommand.ResetType;
import org.eclipse.jgit.api.errors.AbortedByHookException;
import org.eclipse.jgit.api.errors.CanceledException;
import org.eclipse.jgit.api.errors.ConcurrentRefUpdateException;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.api.errors.InvalidRemoteException;
import org.eclipse.jgit.api.errors.NoFilepatternException;
import org.eclipse.jgit.api.errors.NoHeadException;
import org.eclipse.jgit.api.errors.NoMessageException;
import org.eclipse.jgit.api.errors.RefAlreadyExistsException;
import org.eclipse.jgit.api.errors.TransportException;
import org.eclipse.jgit.api.errors.UnmergedPathsException;
import org.eclipse.jgit.api.errors.WrongRepositoryStateException;
import org.eclipse.jgit.lib.Ref;
import org.eclipse.jgit.transport.PushResult;
import org.eclipse.jgit.transport.RemoteRefUpdate;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

public class GitClient {

  private static final Set<PosixFilePermission> ownerReadWrite;

  static {
    ownerReadWrite = new HashSet<>();
    ownerReadWrite.add(PosixFilePermission.OWNER_WRITE);
    ownerReadWrite.add(PosixFilePermission.OWNER_READ);
    ownerReadWrite.add(PosixFilePermission.OWNER_EXECUTE);
    ownerReadWrite.add(PosixFilePermission.GROUP_WRITE);
    ownerReadWrite.add(PosixFilePermission.GROUP_READ);
    ownerReadWrite.add(PosixFilePermission.GROUP_EXECUTE);
  }

  private final Git git;
  private Set<Ref> refs = new HashSet<>();

  public GitClient(Git git) {
    this.git = git;
  }

  /**
   * Clone git repo.
   *
   * @param uri the uri of this repository.
   * @return a new client that it's working directory is temporary
   * @throws IOException if an I/O error occurs or the temporary-file directory does not exist
   * @throws InvalidRemoteException when the {@link CloneCommand#call()} fails
   * @throws TransportException when the {@link CloneCommand#call()} fails
   * @throws GitAPIException when the {@link CloneCommand#call()} fails
   */
  public static GitClient cloneRepo(String uri)
      throws IOException, InvalidRemoteException, TransportException, GitAPIException {

    File dir =
        Files.createTempDirectory("git", PosixFilePermissions.asFileAttribute(ownerReadWrite))
            .toFile();
    return new GitClient(Git.cloneRepository().setDirectory(dir).setURI(uri).call());
  }

  public GitClient pull() {
    try {
      if (!refs.isEmpty()) {
        git.fetch().call();
      }
      git.pull().call();
    } catch (GitAPIException ignoredException) {
      throw new RuntimeException(ignoredException);
    }
    return this;
  }

  public File createFile(String file) throws IOException {
    File f = new File(getWorkingDirectory(), file);
    f.getParentFile().mkdirs();
    f.createNewFile();
    return f;
  }

  public InputStream getFile(String file, boolean createIfNotFound) throws IOException {
    try {
      return new FileInputStream(new File(getWorkingDirectory(), file));
    } catch (FileNotFoundException fnfEx) {
      if (!createIfNotFound) {
        throw fnfEx;
      }
      File f = new File(getWorkingDirectory(), file);
      f.getParentFile().mkdirs();
      f.createNewFile();
      return new FileInputStream(f);
    }
  }

  public OutputStream writeToFile(String file) throws FileNotFoundException {
    return new FileOutputStream(new File(getWorkingDirectory(), file));
  }

  /**
   * push.
   *
   * @return this client
   */
  public Mono<GitClient> push() {
    return Mono.just(git.push())
        .map(
            push -> {
              refs.forEach(push::add);
              return push;
            })
        .flatMapMany(
            push -> {
              try {
                return Flux.fromIterable(push.call());
              } catch (GitAPIException ignoredException) {
                return Flux.error(ignoredException);
              }
            })
        .flatMapIterable(PushResult::getRemoteUpdates)
        .all(
            update ->
                update.getStatus() == RemoteRefUpdate.Status.OK
                    || update.getStatus() == RemoteRefUpdate.Status.UP_TO_DATE)
        .flatMap(
            allIsOk -> {
              if (allIsOk) {
                return Mono.just(this);
              } else {
                return Mono.error(new CanceledException("non-fast-forward"));
              }
            });
  }

  public File getWorkingDirectory() {
    return git.getRepository().getWorkTree();
  }

  public GitClient checkout(String branch) {
    try {
      refs.add(
          git.branchCreate()
              .setName(branch)
              .setUpstreamMode(SetupUpstreamMode.SET_UPSTREAM)
              .call());
      refs.add(
          git.checkout().setName(branch).setUpstreamMode(SetupUpstreamMode.SET_UPSTREAM).call());
    } catch (RefAlreadyExistsException ex) {
      try {
        refs.add(
            git.checkout().setUpstreamMode(SetupUpstreamMode.SET_UPSTREAM).setName(branch).call());
      } catch (GitAPIException ignoredException) {
        throw new RuntimeException(ignoredException);
      }
    } catch (GitAPIException ignoredException) {
      throw new RuntimeException(ignoredException);
    }
    return this;
  }

  public GitClient hardReset() {
    this.refs.forEach(
        ref -> {
          try {
            String resetTo = ref.getLeaf().getName();
            git.reset().setMode(ResetType.HARD).setRef(resetTo).call();
          } catch (GitAPIException ignoredException) {
            throw new RuntimeException(ignoredException);
          }
        });
    return this;
  }

  public GitClient fetchFromOriginToBranch() {
    refs.forEach(
        ref -> {
          try {
            git.fetch()
                .setForceUpdate(true)
                .setRefSpecs(ref.getName() + ":" + ref.getName())
                .call();
          } catch (GitAPIException ignoredException) {
            // TODO Auto-generated catch block
            ignoredException.printStackTrace();
          }
        });
    return this;
  }

  public GitClient add(String files) throws NoFilepatternException, GitAPIException {
    git.add().addFilepattern(files).call();
    return this;
  }

  public GitClient commit(String message)
      throws NoHeadException, NoMessageException, UnmergedPathsException,
          ConcurrentRefUpdateException, WrongRepositoryStateException, AbortedByHookException,
          GitAPIException {
    git.commit().setSign(false).setMessage(message).call();
    return this;
  }
}
