/*******************************************************************************
 * Pentaho Big Data
 * <p>
 * Copyright (C) 2002-2017 by Pentaho : http://www.pentaho.com
 * <p>
 * ******************************************************************************
 * <p>
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance with
 * the License. You may obtain a copy of the License at
 * <p>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p>
 * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on
 * an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations under the License.
 ******************************************************************************/

package org.pentaho.hadoop.shim;

import org.apache.commons.vfs2.AllFileSelector;
import org.apache.commons.vfs2.FileObject;
import org.apache.commons.vfs2.FileType;
import org.apache.commons.vfs2.VFS;
import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Iterator;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

public class HadoopExcludeJarsTest {

  private static String HADOOP_CONFIGURATIONS_PATH = System.getProperty( "java.io.tmpdir" ) + "/exclude-jars";
  private static String HADOOP_CLUSTER_JARS_PATH = System.getProperty( "java.io.tmpdir" ) + "/exclude-cluster-jars";
  private static String PMR_PROPERTIES = "pmr.properties";
  private static File pmrFolder;
  private static URL urlTestResources;
  private int count;


  @ClassRule
  public static TemporaryFolder tempFolder = new TemporaryFolder();

  @BeforeClass
  public static void setup() throws Exception {
    // Create a test hadoop configuration
    FileObject ramRoot = VFS.getManager().resolveFile( HADOOP_CONFIGURATIONS_PATH );
    FileObject ramRootCluster = VFS.getManager().resolveFile( HADOOP_CLUSTER_JARS_PATH );
    if ( ramRoot.exists() ) {
      ramRoot.delete( new AllFileSelector() );
    }
    ramRoot.createFolder();

    if ( ramRootCluster.exists() ) {
      ramRootCluster.delete( new AllFileSelector() );
    }
    ramRootCluster.createFolder();

    // Create the implementation jars
    ramRoot.resolveFile( "xercesImpl-2.9.1.jar" ).createFile();
    ramRoot.resolveFile( "xml-apis-1.3.04.jar" ).createFile();
    ramRoot.resolveFile( "xml-apis-ext-1.3.04.jar" ).createFile();
    ramRoot.resolveFile( "xerces-version-1.8.0.jar" ).createFile();
    ramRoot.resolveFile( "xercesImpl2-2.9.1.jar" ).createFile();
    ramRoot.resolveFile( "pentaho-hadoop-shims-api-61.2016.04.01-196.jar" ).createFile();
    ramRoot.resolveFile( "commands-3.3.0-I20070605-0010.jar" ).createFile();
    ramRoot.resolveFile( "postgresql-9.3-1102-jdbc4.jar" ).createFile();
    ramRoot.resolveFile( "trilead-ssh2-build213.jar" ).createFile();
    ramRoot.resolveFile( "trilead-ssh2-build215.jar" ).createFile();

    ramRootCluster.resolveFile( "hadoop-common-2.7.0-mapr-1607.jar" ).createFile();
    ramRootCluster.resolveFile( "hadoop-mapreduce-client-core-2.7.0-mapr-1506.jar" ).createFile();

    pmrFolder = tempFolder.newFolder( "pmr" );
    urlTestResources = Thread.currentThread().getContextClassLoader().getResource( PMR_PROPERTIES );
    Files.copy( Paths.get( urlTestResources.toURI() ), Paths.get( pmrFolder.getAbsolutePath(), PMR_PROPERTIES ) );
  }

  @Test
  public void filterJars_null_args() throws Exception {
    HadoopConfigurationLocator locator = new HadoopConfigurationLocator();

    List<URL> list = locator.filterJars( null, null );
    assertNull( list );
  }

  @Test
  public void filterJars_null_arg_excludedJarsProperty() throws Exception {
    HadoopConfigurationLocator locator = new HadoopConfigurationLocator();
    FileObject root = VFS.getManager().resolveFile( HADOOP_CONFIGURATIONS_PATH );

    List<URL> urls = locator.parseURLs( root, root.toString() );
    count = urls.size();
    List<URL> list = locator.filterJars( urls, null );
    assertEquals( count, list.size() );
  }

  @Test
  public void filterJars_arg_excludedJarsProperty_emptyString() throws Exception {
    HadoopConfigurationLocator locator = new HadoopConfigurationLocator();
    FileObject root = VFS.getManager().resolveFile( HADOOP_CONFIGURATIONS_PATH );

    List<URL> urls = locator.parseURLs( root, root.toString() );
    count = urls.size();
    List<URL> list = locator.filterJars( urls, "" );
    assertEquals( count, list.size() );
  }

  @Test
  public void filterJars_arg_urls_containsOnlyExcludedJars() throws Exception {
    HadoopConfigurationLocator locator = new HadoopConfigurationLocator();
    FileObject root = VFS.getManager().resolveFile( HADOOP_CONFIGURATIONS_PATH );

    List<URL> urls = locator.parseURLs( root, root.toString() );
    Iterator<URL> iterator = urls.listIterator();
    while ( iterator.hasNext() ) {
      URL url = iterator.next();
      if ( FileType.FOLDER.equals( root.resolveFile( url.toString().trim() ).getType() ) ) {
        iterator.remove();
      }
    }
    List<URL> list = locator.filterJars( urls,
      "xercesImpl,xml-apis-1.3.04.jar,xml-apis-ext-1.3.04,xerces-version-1.8.0,xercesImpl2-2.9.1,"
        + "pentaho-hadoop-shims-api-61.2016.04.01-196,commands-3.3.0-I20070605-0010,postgresql,trilead-ssh2-build213"
        + ".jar,trilead-ssh2-build215.jar" );
    assertEquals( 0, list.size() );
  }

  @Test
  public void filterJars_removeOnlyXercesImpl() throws Exception {
    HadoopConfigurationLocator locator = new HadoopConfigurationLocator();
    FileObject root = VFS.getManager().resolveFile( HADOOP_CONFIGURATIONS_PATH );

    List<URL> urls = locator.parseURLs( root, root.toString() );
    count = urls.size();
    List<URL> list = locator.filterJars( urls, "xercesImpl" );
    assertEquals( count - 1, list.size() );
  }

  @Test
  public void filterJars_removeOnlyByArtifactIdTemplate() throws Exception {
    HadoopConfigurationLocator locator = new HadoopConfigurationLocator();
    FileObject root = VFS.getManager().resolveFile( HADOOP_CONFIGURATIONS_PATH );

    List<URL> urls = locator.parseURLs( root, root.toString() );
    count = urls.size();
    List<URL> list = locator.filterJars( urls, "pentaho-hadoop-shims-api" );
    assertEquals( count - 1, list.size() );
  }

  @Test
  public void filterJars_removeOnlyByArtifactIdVersionTemplate() throws Exception {
    HadoopConfigurationLocator locator = new HadoopConfigurationLocator();
    FileObject root = VFS.getManager().resolveFile( HADOOP_CONFIGURATIONS_PATH );

    List<URL> urls = locator.parseURLs( root, root.toString() );
    count = urls.size();
    List<URL> list = locator.filterJars( urls, "pentaho-hadoop-shims-api-61.2016.04.01-196" );
    assertEquals( count - 1, list.size() );
  }

  @Test
  public void filterJars_removeOnlyByWholeJarName() throws Exception {
    HadoopConfigurationLocator locator = new HadoopConfigurationLocator();
    FileObject root = VFS.getManager().resolveFile( HADOOP_CONFIGURATIONS_PATH );

    List<URL> urls = locator.parseURLs( root, root.toString() );
    count = urls.size();
    List<URL> list = locator.filterJars( urls, "pentaho-hadoop-shims-api-61.2016.04.01-196.jar" );
    assertEquals( count - 1, list.size() );
  }

  @Test
  public void filterJars_removeThreeJarsWithDifferentTemplates() throws Exception {
    HadoopConfigurationLocator locator = new HadoopConfigurationLocator();
    FileObject root = VFS.getManager().resolveFile( HADOOP_CONFIGURATIONS_PATH );

    List<URL> urls = locator.parseURLs( root, root.toString() );
    count = urls.size();
    List<URL> list =
      locator.filterJars( urls, "xercesImpl,trilead-ssh2-build215,pentaho-hadoop-shims-api-61.2016.04.01-196.jar" );
    assertEquals( count - 3, list.size() );
  }

  private void activatePmrFile() throws URISyntaxException, IOException {
    if ( !Files.exists( Paths.get( urlTestResources.toURI() ) ) ) {
      Files.copy( Paths.get( pmrFolder.getAbsolutePath(), PMR_PROPERTIES ), Paths.get( urlTestResources.toURI() ) );
    }
  }

  private void disablePmrFile() throws URISyntaxException, IOException {
    Files.deleteIfExists( Paths.get( urlTestResources.toURI() ) );
  }

  @Test
  public void filterClusterJars_isPmrTrue_null_args() throws Exception {
    HadoopConfigurationLocator locator = new HadoopConfigurationLocator();

    activatePmrFile();
    List<URL> list = locator.filterClusterJars( null, null );
    assertNull( list );
  }

  @Test
  public void filterClusterJars_isPmrTrue_arg_excludedJarsProperty_emptyString() throws Exception {
    HadoopConfigurationLocator locator = new HadoopConfigurationLocator();

    activatePmrFile();
    FileObject root = VFS.getManager().resolveFile( HADOOP_CLUSTER_JARS_PATH );
    List<URL> urls = locator.parseURLs( root, root.toString() );

    count = urls.size();
    List<URL> list = locator.filterClusterJars( urls, "" );
    assertEquals( count, list.size() );
  }

  @Test
  public void filterClusterJars_isPmrTrue_removeOnlyHadoopCommon() throws Exception {
    HadoopConfigurationLocator locator = new HadoopConfigurationLocator();

    activatePmrFile();
    FileObject root = VFS.getManager().resolveFile( HADOOP_CLUSTER_JARS_PATH );
    List<URL> urls = locator.parseURLs( root, root.toString() );
    boolean containsJar = false;

    count = urls.size();
    List<URL> list = locator.filterClusterJars( urls, "hadoop-common" );
    assertEquals( count - 1, list.size() );

    for ( URL url : list ) {
      if ( url.getPath().contains( "hadoop-common" ) ) {
        containsJar = true;
        break;
      }
    }
    assertEquals( false, containsJar );
  }

  @Test
  public void filterClusterJars_isPmrTrue_arg_urls_containsOnlyExcludedJars() throws Exception {
    HadoopConfigurationLocator locator = new HadoopConfigurationLocator();

    activatePmrFile();
    FileObject root = VFS.getManager().resolveFile( HADOOP_CLUSTER_JARS_PATH );
    List<URL> urls = locator.parseURLs( root, root.toString() );
    Iterator<URL> iterator = urls.listIterator();
    while ( iterator.hasNext() ) {
      URL url = iterator.next();
      if ( FileType.FOLDER.equals( root.resolveFile( url.toString().trim() ).getType() ) ) {
        iterator.remove();
      }
    }

    count = urls.size();
    List<URL> list =
      locator.filterClusterJars( urls, "hadoop-common-2.7.0-mapr-1607.jar,hadoop-client-2.7.0-mapr-1607.jar" );
    assertEquals( 1, list.size() );
  }

  @Test
  public void filterClusterJars_isPmrFalse_arg_excludedJarsProperty_emptyString() throws Exception {
    HadoopConfigurationLocator locator = new HadoopConfigurationLocator();

    try {
      disablePmrFile();
      FileObject root = VFS.getManager().resolveFile( HADOOP_CLUSTER_JARS_PATH );
      List<URL> urls = locator.parseURLs( root, root.toString() );

      count = urls.size();
      List<URL> list = locator.filterClusterJars( urls, "" );
      assertEquals( count, list.size() );
    } finally {
      activatePmrFile();
    }
  }

  @Test
  public void filterClusterJars_isPmrFalse_null_args() throws Exception {
    HadoopConfigurationLocator locator = new HadoopConfigurationLocator();

    try {
      disablePmrFile();
      List<URL> list = locator.filterClusterJars( null, null );
      assertNull( list );
    } finally {
      activatePmrFile();
    }
  }
}
