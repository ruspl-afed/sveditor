package net.sf.sveditor.core.db.index.plugin;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

import net.sf.sveditor.core.SVCorePlugin;
import net.sf.sveditor.core.db.index.ISVDBFileSystemChangeListener;
import net.sf.sveditor.core.db.index.ISVDBFileSystemProvider;
import net.sf.sveditor.core.log.LogFactory;
import net.sf.sveditor.core.log.LogHandle;

import org.osgi.framework.Bundle;
import org.osgi.framework.Version;

public class SVDBPluginFileSystemProvider implements ISVDBFileSystemProvider {
	private Bundle				fBundle;
	private String				fPluginNS;
	private LogHandle			fLog;
	private long				fBundleVersion;
	
	public SVDBPluginFileSystemProvider(
			Bundle				bundle,
			String				plugin_ns) {
		fBundle = bundle;
		fPluginNS = plugin_ns;
		fLog = LogFactory.getLogHandle("SVDBPluginFileSystemProvider");
		fBundleVersion = -1;
	}

	@Override
	public void init(String root) { }

	@Override
	public void dispose() { }

	@Override
	public void addMarker(String path, String type, int lineno, String msg) { }

	@Override
	public void clearMarkers(String path) { }

	@Override
	public String resolvePath(String path, String fmt) {
		return path;
	}

	@Override
	public boolean fileExists(String path) {
 		if (path.startsWith("plugin:/")) {

			String leaf = path.substring(("plugin:/" + fPluginNS).length());

			return (fBundle.getEntry(leaf) != null);
		} else {
			return false;
		}
	}

	@Override
	public boolean isDir(String path) {
 		if (path.startsWith("plugin:/")) {
 			URL entry;

			String leaf = path.substring(("plugin:/" + fPluginNS).length());

			return ((entry = fBundle.getEntry(leaf)) != null && entry.getPath().endsWith("/"));
		} else {
			return false;
		}		
	}

	@Override
	public List<String> getFiles(String path) {
		// TODO: Don't support this currently for plugin library
		return new ArrayList<String>();
	}

	@Override
	public InputStream openStream(String path) {
		InputStream ret = null;
		
		if (path.startsWith("plugin:/")) {

			String leaf = path.substring(("plugin:/" + fPluginNS).length());

			URL url = fBundle.getEntry(leaf);

			if (url != null) {
				try {
					ret = url.openStream();
				} catch (IOException e) {
					fLog.error("Failed to open plugin file \"" + 
							path + "\"", e);
				}
			}
		}
		
		return ret;
	}

	@Override
	public OutputStream openStreamWrite(String path) {
		return null;
	}

	@Override
	public void closeStream(InputStream in) {
		try {
			in.close();
		} catch (IOException e) {}
	}

	@Override
	public void closeStream(OutputStream in) {
		try {
			in.close();
		} catch (IOException e) {}
	}

	@Override
	public long getLastModifiedTime(String path) {
		if (fBundleVersion == -1) {
			Version v = SVCorePlugin.getDefault().getBundle().getVersion();
			// Ensure the version is always really big to work around the temporary 
			// current problem of having existing versions out in the wild
			fBundleVersion  = v.getMicro();
			fBundleVersion |= v.getMinor() << 16;
			fBundleVersion |= v.getMajor() << 24;
			fBundleVersion |= (8L << 48L);
		}
		if (fBundleVersion < fBundle.getLastModified()) {
			System.out.println("computed bundle version older than BundleVersion: " +
					fBundleVersion + " " + fBundle.getLastModified());
		}
		
		return fBundleVersion;		
	}

	@Override
	public void addFileSystemChangeListener(ISVDBFileSystemChangeListener l) { }

	@Override
	public void removeFileSystemChangeListener(ISVDBFileSystemChangeListener l) { }

}
