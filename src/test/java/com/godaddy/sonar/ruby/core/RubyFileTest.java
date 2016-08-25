package com.godaddy.sonar.ruby.core;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.sonar.api.batch.fs.InputFile;
import org.sonar.api.batch.fs.internal.DefaultInputFile;
import org.sonar.api.resources.Qualifiers;
import org.sonar.api.resources.Scopes;

public class RubyFileTest {
	protected final static String SOURCE_FILE = "/path/to/source/file.rb";
	
	protected RubyFile rubyFile;
	
	@Before
	public void setUp() {
		File file = new File(SOURCE_FILE);
		List<InputFile> sourceDirs = new ArrayList<InputFile>();

//		fs.add(new DefaultInputFile(file.getPath()).setAbsolutePath(file.getAbsolutePath()).setType(Type.MAIN).setLanguage(Java.KEY));
		File aSrcDir = new File("/path/to/source");
//		sourceDirs.add(new DefaultInputFile("/path/to/source"));
        sourceDirs.add(
        		new DefaultInputFile(
        				"",
						aSrcDir.getPath()).setModuleBaseDir(file.getParentFile().toPath()));
		
		rubyFile = new RubyFile(file, sourceDirs);
	}
	
	@After 
	public void tearDown() {
		
	}
	
	

	@Test(expected=IllegalArgumentException.class)
	public void testRubyFileWithNullFile() {
		new RubyFile(null, new ArrayList<InputFile>());
	}
	
	@Test
	public void testRubyFileWithNullSourceDirs() {
		File file = new File(SOURCE_FILE);
		rubyFile = new RubyFile(file, null);
		assertEquals("[default].file", rubyFile.getKey());
	}
	
	@Test
	public void testGetParent() {
		RubyPackage parent = rubyFile.getParent();
		assertEquals("source", parent.getKey());
	}

	@Test
	public void testGetDescription() {
		assertNull(rubyFile.getDescription());
	}

	@Test
	public void testGetLanguage() {
		assertEquals(Ruby.INSTANCE, rubyFile.getLanguage());
	}

	@Test
	public void testGetName() {
		assertEquals("file", rubyFile.getName());
	}

	@Test
	public void testGetLongName() {
		assertEquals("source.file", rubyFile.getLongName());
	}

	@Test
	public void testGetScope() {
		assertEquals(Scopes.FILE, rubyFile.getScope());
	}

	@Test
	public void testGetQualifier() {
		assertEquals(Qualifiers.CLASS, rubyFile.getQualifier());
	}

	@Test
	public void testMatchFilePatternString() {
		assertTrue(rubyFile.matchFilePattern("source.file.rb"));
	}

//	@Test
//	public void testToString() {
//		System.out.println(rubyFile.toString());
//		assertTrue(rubyFile.toString().contains("key=source.file,package=source,longName=source.file"));
//	}

}
