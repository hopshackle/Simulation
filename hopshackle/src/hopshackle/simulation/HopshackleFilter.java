package hopshackle.simulation;

import java.io.*;

public class HopshackleFilter implements FilenameFilter{

	private String searchText;
	private String suffix;
	public HopshackleFilter(String containsText, String suffix) {
		searchText = containsText;
		this.suffix = suffix;
	}
	public boolean accept(File dir, String fileName) {
		if (!fileName.endsWith("." + suffix)) 
			return false;

		if (searchText != null && fileName.contains(searchText))
			return true;

		return false;
	}
}
