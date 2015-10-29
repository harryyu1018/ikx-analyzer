package org.wltea.analyzer.cfg;

import java.util.List;

import org.testng.annotations.Test;

public class DefaultConfigTest {

	@Test
	public void getExtDictionarys() {
		
		Configuration conf = DefaultConfig.getInstance();
		List<String> exts = conf.getExtDictionarys();
		
		if (exts != null) {
			for (String ext : exts) {
				System.out.println(ext);
			}
		}
	}
	
	@Test
	public void getExtStopWordDictionarys() {
		
		Configuration conf = DefaultConfig.getInstance();
		List<String> exts = conf.getExtStopWordDictionarys();
		
		if (exts != null) {
			for (String ext : exts) {
				System.out.println(ext);
			}
		}
		
	}
}
