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
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.ResetCommand.ResetType;
import org.eclipse.jgit.api.errors.AbortedByHookException;
import org.eclipse.jgit.api.errors.CanceledException;
import org.eclipse.jgit.api.errors.CheckoutConflictException;
import org.eclipse.jgit.api.errors.ConcurrentRefUpdateException;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.api.errors.InvalidConfigurationException;
import org.eclipse.jgit.api.errors.InvalidRefNameException;
import org.eclipse.jgit.api.errors.InvalidRemoteException;
import org.eclipse.jgit.api.errors.NoFilepatternException;
import org.eclipse.jgit.api.errors.NoHeadException;
import org.eclipse.jgit.api.errors.NoMessageException;
import org.eclipse.jgit.api.errors.RefAlreadyExistsException;
import org.eclipse.jgit.api.errors.RefNotAdvertisedException;
import org.eclipse.jgit.api.errors.RefNotFoundException;
import org.eclipse.jgit.api.errors.TransportException;
import org.eclipse.jgit.api.errors.UnmergedPathsException;
import org.eclipse.jgit.api.errors.WrongRepositoryStateException;

public class GitClient {

  private static final Set<PosixFilePermission> ownerReadWrite;

  static {
    ownerReadWrite = new HashSet<>();
    ownerReadWrite.add(PosixFilePermission.OWNER_WRITE);
    ownerReadWrite.add(PosixFilePermission.OWNER_READ);
  }

  private final Git git;

  public GitClient(Git git) {
    this.git = git;
  }

  /**
   * Clone git repo.
   *
   * @param uri the uri of this repository.
   * @return a new client that it's working directory is temporary
   * @throws IOException
   * @throws InvalidRemoteException
   * @throws TransportException
   * @throws GitAPIException
   */
  public static GitClient cloneRepo(String uri)
      throws IOException, InvalidRemoteException, TransportException, GitAPIException {

    File dir =
        Files.createTempDirectory("git", PosixFilePermissions.asFileAttribute(ownerReadWrite))
            .toFile();
    return new GitClient(Git.cloneRepository().setDirectory(dir).setURI(uri).call());
  }

  public GitClient pull()
      throws WrongRepositoryStateException, InvalidConfigurationException, InvalidRemoteException,
          CanceledException, RefNotFoundException, RefNotAdvertisedException, NoHeadException,
          TransportException, GitAPIException {
    git.pull().call();
    return this;
  }

  public File createFile(String file) throws IOException {
    File f = new File(git.getRepository().getDirectory(), file);
    f.getParentFile().mkdirs();
    f.createNewFile();
    return f;
  }

  public InputStream getFile(String file, boolean createIfNotFound) throws IOException {
    try {
      return new FileInputStream(new File(git.getRepository().getDirectory(), file));
    } catch (FileNotFoundException fnfEx) {
      if (!createIfNotFound) {
        throw fnfEx;
      }
      File f = new File(git.getRepository().getDirectory(), file);
      f.getParentFile().mkdirs();
      f.createNewFile();
      return new FileInputStream(f);
    }
  }

  public OutputStream writeToFile(String file) throws FileNotFoundException {
    return new FileOutputStream(new File(git.getRepository().getDirectory(), file));
  }

  public GitClient push() throws InvalidRemoteException, TransportException, GitAPIException {
    git.push().call();
    return this;
  }

  public File getWorkingDirectory() {
    return git.getRepository().getDirectory();
  }

  public GitClient checkout(String branch)
      throws RefAlreadyExistsException, RefNotFoundException, InvalidRefNameException,
          CheckoutConflictException, GitAPIException {
    git.checkout().setStartPoint("master").setCreateBranch(true).addPath(branch).call();
    return this;
  }

  public GitClient hardReset() throws CheckoutConflictException, GitAPIException {
    git.reset().setMode(ResetType.HARD).call();
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
    git.commit().setMessage(message).call();
    return this;
  }
}
