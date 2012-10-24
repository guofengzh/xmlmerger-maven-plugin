/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.planner.xmlmerger;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;

import ch.elca.el4j.services.xmlmerge.AbstractXmlMergeException;
import ch.elca.el4j.services.xmlmerge.ConfigurationException;
import ch.elca.el4j.services.xmlmerge.XmlMerge;
import ch.elca.el4j.services.xmlmerge.config.ConfigurableXmlMerge;
import ch.elca.el4j.services.xmlmerge.config.PropertyXPathConfigurer;

/**
 * Merges multiple XML files into one.
 *

 * @goal merge
 * @phase process-sources
 * @requiresProject
 */
public class MergeMojo extends AbstractMojo {
	
	private static final String DEFAULT_ENCODING = "UTF-8";

	/**
	 * The XML files to merge. <br>
	 * 
	 * @parameter
	 * @required
	 */
	private Merge[] merges;

	/**
	 * @see org.apache.maven.plugin.AbstractMojo#execute()
	 */
	public void execute() throws MojoExecutionException, MojoFailureException {
		for (Merge merge : merges) {
			
			// remove target file if present...
			//if (merge.getTargetFile().exists()) {
			//	merge.getTargetFile().delete();
			//}

			// merge the files into the target directory
			int numMergedFiles;
			try {
				numMergedFiles = mergeFiles(merge);
				getLog().info("Finished Appending: " + numMergedFiles + " files to the target file: " + merge.getTargetFile().getAbsolutePath() + ".");
			} catch (IOException e) {
				throw new  MojoExecutionException(e.getMessage());
			}

		}
	}

	private int mergeFiles(Merge merge) throws MojoExecutionException, IOException {

		// Get the files to merge
		List<FileInputStream> streamsToMerge = new ArrayList<FileInputStream>();

		for (File f : merge.getFiles() ) {
		  streamsToMerge.add(new FileInputStream( f ) ) ;
		}

		// Create the stream to write
		File destFile = merge.getTargetFile() ;
		OutputStream out = new FileOutputStream(destFile);

		// Create conf properties
		Properties confProps = new Properties();
		if ( merge.getPropertyFile() != null ) {
			InputStream configIn = null ;
			try {
				configIn = new FileInputStream(  merge.getPropertyFile() );
				confProps.load(configIn);
			} finally {
				if ( configIn != null )
					configIn.close() ;
			}
		}

		// Create the XmlMerge instance and execute the merge
		XmlMerge xmlMerge;
		try {
			xmlMerge = new ConfigurableXmlMerge(new PropertyXPathConfigurer(confProps));
		} catch (ConfigurationException e) {
			throw new MojoExecutionException(e.getMessage());
		}

		InputStream in = null ;

		try {
			in = xmlMerge.merge((InputStream[]) streamsToMerge.toArray(new InputStream[streamsToMerge.size()]));
		} catch (AbstractXmlMergeException e) {
			throw new  MojoExecutionException(e.getMessage());
		}

		writeFromTo(in, out);

		try {
			in.close();
		} catch (IOException e) {
			throw new MojoExecutionException(e.getMessage());
		}

		try {
			out.close();
		} catch (IOException e) {
			throw new MojoExecutionException(e.getMessage());
		}
		
		return streamsToMerge.size() ;
	}

	/**
	 * Writes from an InputStream to an OutputStream.
	 * @param in The stream to read from
	 * @param out The stream to write to
	 * @throws BuildException If an error occurred during the write process
	 */
	private void writeFromTo(InputStream in, OutputStream out) throws MojoExecutionException {
		int len = 0;
		byte[] buffer = new byte[1024];

		try {
			while ((len = in.read(buffer)) != -1) {
				out.write(buffer, 0, len);
			}
		} catch (IOException ioe) {
			throw new MojoExecutionException(ioe.getMessage());
		}
	}
}
