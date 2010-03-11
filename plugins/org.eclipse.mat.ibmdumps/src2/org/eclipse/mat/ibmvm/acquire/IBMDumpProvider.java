/*******************************************************************************
 * Copyright (c) 2010 IBM Corporation
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *    IBM Corporation - initial implementation
 *******************************************************************************/
package org.eclipse.mat.ibmvm.acquire;

import java.io.File;
import java.io.FileFilter;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.Serializable;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Properties;

import org.eclipse.mat.SnapshotException;
import org.eclipse.mat.snapshot.acquire.VmInfo;
import org.eclipse.mat.util.IProgressListener;
import org.eclipse.mat.util.IProgressListener.Severity;

import com.ibm.tools.attach.AgentInitializationException;
import com.ibm.tools.attach.AgentLoadException;
import com.ibm.tools.attach.AttachNotSupportedException;
import com.ibm.tools.attach.VirtualMachine;
import com.ibm.tools.attach.VirtualMachineDescriptor;

/**
 * Base class for generating dumps on IBM VMs
 * @author ajohnson
 *
 */
public abstract class IBMDumpProvider extends BaseProvider
{

    /**
     * Find new files not ones we know about
     */
    private static final class NewFileFilter implements FileFilter
    {
        private final Collection<File> previousFiles;

        private NewFileFilter(Collection<File> previousFiles)
        {
            this.previousFiles = previousFiles;
        }

        public boolean accept(File f)
        {
            return !previousFiles.contains(f);
        }
    }

    private static final class StderrProgressListener implements IProgressListener
    {
        public void beginTask(String name, int totalWork)
        {}

        public void done()
        {}

        public boolean isCanceled()
        {
            return false;
        }

        public void sendUserMessage(Severity severity, String message, Throwable exception)
        {}

        public void setCanceled(boolean value)
        {}

        public void subTask(String name)
        {}

        public void worked(int work)
        {
            for (int i = 0; i < work; ++i)
            {
                System.err.print('.');
            }
        }
    }

    /**
     * sorter for files by date modified
     */
    private static final class FileComparator implements Comparator<File>, Serializable
    {
        /**
         * 
         */
        private static final long serialVersionUID = -3725792252276130382L;

        public int compare(File f1, File f2)
        {
            return Long.valueOf(f1.lastModified()).compareTo(Long.valueOf(f2.lastModified()));
        }
    }

    final VirtualMachineDescriptor vmd;

    public IBMDumpProvider(VirtualMachineDescriptor vmd)
    {
        this.vmd = vmd;
    }
    
    public IBMDumpProvider()
    {
        this(null);
    }

    /**
     * Command to pass to the agent to generate dumps of this type
     * @return
     */
    abstract String agentCommand();

    /**
     * Suggested name for dumps of this type
     * @return
     */
    abstract String dumpName();

    private static File agentJar;

    /**
     * Number of files generated by this dump type
     * @return
     */
    abstract int files();

    File jextract(File preferredDump, File dump, File udir, File javahome, IProgressListener listener)
                    throws IOException, InterruptedException, SnapshotException
    {
        return dump;
    }

    /**
     * Average file length for a group of files.
     * @param files
     * @return
     */
    long averageFileSize(Collection<File> files)
    {
        long l = 0;
        int i = 0;
        for (File f : files)
        {
            if (f.isFile())
            {
                l += f.length();
                ++i;
            }
        }
        return l / i;
    }

    public File acquireDump(VmInfo info, File preferredLocation, IProgressListener listener) throws SnapshotException
    {
        listener.beginTask(Messages.getString("IBMDumpProvider.GeneratingDump"), TOTAL_WORK); //$NON-NLS-1$
        try
        {
            listener.subTask(MessageFormat.format(Messages.getString("IBMDumpProvider.AttachingToVM"), vmd.id())); //$NON-NLS-1$
            VirtualMachine vm = vmd.provider().attachVirtualMachine(vmd);
            try
            {
                Properties props = vm.getSystemProperties();
                String javah = props.getProperty("java.home", System.getProperty("java.home")); //$NON-NLS-1$ //$NON-NLS-2$
                File javahome = new File(javah);

                // Where the dumps end up
                // IBM_HEAPDUMPDIR
                // IBM_JAVACOREDIR
                // pwd
                // TMPDIR
                // /tmp
                String userdir = props.getProperty("user.dir", System.getProperty("user.dir")); //$NON-NLS-1$ //$NON-NLS-2$

                File udir = new File(userdir);
                File f1[] = udir.listFiles();
                Collection<File> previous = new HashSet<File>(Arrays.asList(f1));

                long avg = averageFileSize(previous);
                //System.err.println("Average = " + avg);

                try
                {
                    String jar = null;

                    jar = getAgentJar().getAbsolutePath();

                    listener.subTask(Messages.getString("IBMDumpProvider.StartingAgent")); //$NON-NLS-1$
                    vm.loadAgent(jar, agentCommand());
                }
                catch (AgentLoadException e2)
                {
                    listener.sendUserMessage(Severity.WARNING, Messages.getString("IBMDumpProvider.AgentLoad"), e2); //$NON-NLS-1$
                    throw new SnapshotException(Messages.getString("IBMDumpProvider.AgentLoad"), e2); //$NON-NLS-1$
                }
                catch (AgentInitializationException e)
                {
                    listener.sendUserMessage(Severity.WARNING, Messages.getString("IBMDumpProvider.AgentInitialization"), e); //$NON-NLS-1$
                    throw new SnapshotException(Messages.getString("IBMDumpProvider.AgentInitialization"), e); //$NON-NLS-1$
                }

                List<File> newFiles = progress(udir, previous, files(), avg, listener);
                if (listener.isCanceled())
                    return null;

                File dump = newFiles.get(0);
                listener.done();
                return jextract(preferredLocation, dump, udir, javahome, listener);
            }
            catch (InterruptedException e)
            {
                listener.sendUserMessage(Severity.WARNING, Messages.getString("IBMDumpProvider.Interrupted"), e); //$NON-NLS-1$
                throw new SnapshotException(Messages.getString("IBMDumpProvider.Interrupted"), e); //$NON-NLS-1$
            }
            finally {
                vm.detach();
            }
        }
        catch (AttachNotSupportedException e)
        {
            listener.sendUserMessage(Severity.WARNING, Messages.getString("IBMDumpProvider.UnsuitableVM"), e); //$NON-NLS-1$
            info.setHeapDumpEnabled(false);
            throw new SnapshotException(Messages.getString("IBMDumpProvider.UnsuitableVM"), e); //$NON-NLS-1$
        }
        catch (IOException e)
        {
            listener.sendUserMessage(Severity.WARNING, Messages.getString("IBMDumpProvider.UnableToGenerateDump"), e); //$NON-NLS-1$
            throw new SnapshotException(Messages.getString("IBMDumpProvider.UnableToGenerateDump"), e); //$NON-NLS-1$
        }

    }

    /**
     * Update the progress bar for created files
     */
    private List<File> progress(File udir, Collection<File> previous, int nfiles, long avg, IProgressListener listener)
                    throws InterruptedException
    {
        listener.subTask(Messages.getString("IBMDumpProvider.WaitingForDumpFiles")); //$NON-NLS-1$
        List<File> newFiles = new ArrayList<File>();
        // Wait up to 30 seconds for a file to be created and written to
        long l = 0;
        int worked = 0;
        long start = System.currentTimeMillis(), t;
        for (int i = 0; (l = fileLengths(udir, previous, newFiles, nfiles)) == 0 
            && i < CREATE_COUNT && (t = System.currentTimeMillis()) < start + CREATE_COUNT*SLEEP_TIMEOUT; ++i)
        {
            Thread.sleep(SLEEP_TIMEOUT);
            if (listener.isCanceled()) 
                return null;
            int towork = (int)Math.min(((t - start) / SLEEP_TIMEOUT), CREATE_COUNT);
            listener.worked(towork - worked);
            worked = towork;
        }

        listener.worked(CREATE_COUNT - worked);
        worked = CREATE_COUNT;

        // Wait for FINISHED_TIMEOUT seconds after file length stops changing
        long l0 = l - 1;
        int iFile = 0;
        start = System.currentTimeMillis();
        for (int i = 0, j = 0; ((l = fileLengths(udir, previous, newFiles, nfiles)) != l0
                        || j++ < FINISHED_COUNT || newFiles.size() > iFile)
                        && i < GROW_COUNT
                        && (t = System.currentTimeMillis()) < start + GROW_COUNT*SLEEP_TIMEOUT; ++i)
        {
            while (iFile < newFiles.size())
            {
                listener.subTask(MessageFormat.format(Messages.getString("IBMDumpProvider.WritingFile"), newFiles.get(iFile++))); //$NON-NLS-1$
            }
            if (l0 != l)
            {
                j = 0;
                int towork = (int) (l * GROWING_COUNT / avg);
                listener.worked(towork - worked);
                worked = towork;
                l0 = l;
            }
            Thread.sleep(SLEEP_TIMEOUT);
            if (listener.isCanceled())
                return null;
            listener.worked(1);
        }
        return newFiles;
    }

    private static synchronized File getAgentJar() throws IOException
    {
        if (agentJar == null || !agentJar.canRead())
        {
            agentJar = makeAgentJar();
        }
        return agentJar;
    }

    private static File makeAgentJar() throws IOException, FileNotFoundException
    {
        String jarname = "org.eclipse.mat.ibmdumps"; //$NON-NLS-1$
        String agents[] = { "org.eclipse.mat.ibmvm.agent.DumpAgent" }; //$NON-NLS-1$
        Class<?> cls[] = new Class<?>[0];
        return makeJar(jarname, "Agent-class: ", agents, cls); //$NON-NLS-1$
    }

    /**
     * Find the new files in a directory
     * 
     * @param udir
     *            The directory
     * @param previousFiles
     *            File that we already know exist in the directory
     * @param newFiles
     *            newly discovered files, in discovery/modification order
     * @return
     */
    public List<File> files(File udir, final Collection<File> previousFiles, List<File> newFiles)
    {
        File f2[] = udir.listFiles(new NewFileFilter(previousFiles));
        List<File> new2 = Arrays.asList(f2);
        // Sort the new files in order of modification
        Collections.sort(new2, new FileComparator());
        previousFiles.addAll(new2);
        newFiles.addAll(new2);
        return newFiles;
    }

    public long fileLengths(File udir, Collection<File> previous, List<File> newFiles, int maxFiles)
    {
        Collection<File> nw = files(udir, previous, newFiles);
        long l = 0;
        int i = 0;
        for (File f : nw)
        {
            if (++i > maxFiles)
                break;
            l += f.length();
        }
        return l;
    }

    public List<VmInfo> getAvailableVMs()
    {
        List<VirtualMachineDescriptor> list = com.ibm.tools.attach.VirtualMachine.list();
        List<VmInfo> jvms = new ArrayList<VmInfo>();
        for (VirtualMachineDescriptor vmd : list)
        {
            boolean usable = true;
            // See if the VM is usable to get dumps
            if (false) try {
                // Hope that this is not too intrusive to the target
                VirtualMachine vm = vmd.provider().attachVirtualMachine(vmd);
                try {
                } finally {
                    vm.detach();
                }
            } catch (AttachNotSupportedException e) {
                usable = false;
            } catch (IOException e) {
                usable = false;
            }
            // See if loading an agent would fail
            try {
                // Java 5 SR10 and SR11 don't have a loadAgent method, so find out now
                VirtualMachine.class.getMethod("loadAgent", String.class, String.class); //$NON-NLS-1$
            } catch (NoSuchMethodException e) {
                return null;
            }
            
            // Create VMinfo to generate heap dumps
            VmInfo ifo = new VmInfo();
            String desc1 = MessageFormat.format(Messages.getString("IBMDumpProvider.HeapDumpDescription"), vmd.provider().name(), vmd.provider().type(), vmd.displayName()); //$NON-NLS-1$
            ifo.setDescription(desc1);
            ifo.setPid(processID(vmd));
            IBMHeapDumpProvider heapDumpProvider = new IBMHeapDumpProvider(vmd);
            ifo.setProposedFileName(heapDumpProvider.dumpName());
            ifo.setHeapDumpProvider(heapDumpProvider);
            ifo.setHeapDumpEnabled(usable);
            jvms.add(ifo);

            // Create VMinfo to generate system dumps
            ifo = new VmInfo();
            String desc2 = MessageFormat.format(Messages.getString("IBMDumpProvider.SystemDumpDescription"), vmd.provider().name(), vmd.provider().type(), vmd.displayName()); //$NON-NLS-1$
            ifo.setDescription(desc2);
            ifo.setPid(processID(vmd));
            IBMSystemDumpProvider systemDumpProvider = new IBMSystemDumpProvider(vmd);
            ifo.setProposedFileName(systemDumpProvider.dumpName());
            ifo.setHeapDumpProvider(systemDumpProvider);
            ifo.setHeapDumpEnabled(usable);
            jvms.add(ifo);
        }
        return jvms;
    }

    /**
     * The process id is sometimes 4321.1
     * 
     * @param vmd
     * @return
     */
    private int processID(VirtualMachineDescriptor vmd)
    {
        String id = vmd.id();
        return getPid(id);
    }

    /**
     * Lists VMs or acquires a dump.
     * Used when attach API not usable from the MAT process.
     * 
     * @param s[0]
     *            VM type s[1] dump name
     */
    public static void main(String s[]) throws Exception
    {
        VMListDumpProvider prov = new VMListDumpProvider();
        List<VmInfo> vms = prov.getAvailableVMs();
        IProgressListener ii = new StderrProgressListener();
        for (VmInfo info : vms)
        {
            String vm = info.getPid() + "," + info.getDescription(); //$NON-NLS-1$
            String vm2 = info.getPid() + "," + info.getProposedFileName() + "," + info.getDescription();  //$NON-NLS-1$//$NON-NLS-2$
            if (s.length < 2)
            {
                System.out.println(vm2);
            }
            else
            {
                if (vm.equals(s[0]))
                {
                    File f2 = info.getHeapDumpProvider().acquireDump(info, new File(s[1]), ii);
                    System.out.println(f2.getPath());
                    return;
                }
            }
        }
        if (s.length > 0)
        {
            throw new IllegalArgumentException("No VM found to match " + s[0]);//$NON-NLS-1$
        }
    }
}
