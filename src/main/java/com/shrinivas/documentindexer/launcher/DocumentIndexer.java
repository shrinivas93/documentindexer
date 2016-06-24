package com.shrinivas.documentindexer.launcher;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FilenameFilter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;

import org.apache.commons.io.FilenameUtils;
import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import com.shrinivas.documentindexer.document.Index;
import com.shrinivas.documentindexer.repository.IndexRepository;

@Component
public class DocumentIndexer {

	private static final Logger LOGGER = LogManager.getLogger(DocumentIndexer.class);

	@Autowired
	private IndexRepository indexRepository;

	@Value("${source.file}")
	private String sourceFile;

	public void process() throws IOException {
		List<File> indexableFiles = getSourceFiles();
		LOGGER.info("Found " + indexableFiles.size() + " indexable files.");
		Map<String, Set<String>> index = convertIndexToMap(indexRepository.findAll());
		index = startIndexing(indexableFiles, index);
		LOGGER.info("Indexed " + index.size() + " words");
		List<Index> indices = convertMapToDocument(index);
		LOGGER.info("Storing indices to DB");
		indexRepository.save(indices);
	}

	private Map<String, Set<String>> convertIndexToMap(List<Index> findAll) {
		Map<String, Set<String>> indices = new TreeMap<>();
		for (Index index : findAll) {
			indices.put(index.getWord(), index.getFiles());
		}
		return indices;
	}

	private List<Index> convertMapToDocument(Map<String, Set<String>> index) {
		List<Index> indices = new ArrayList<>();
		Set<String> words = index.keySet();
		for (String word : words) {
			indices.add(new Index(word, index.get(word)));
		}
		return indices;
	}

	private Map<String, Set<String>> startIndexing(List<File> indexableFiles, Map<String, Set<String>> index)
			throws IOException {
		for (File file : indexableFiles) {
			String filePath = file.getAbsolutePath();
			try {
				FileReader fr = new FileReader(file);
				BufferedReader br = new BufferedReader(fr);
				String str;
				while ((str = br.readLine()) != null) {
					String[] words = str.split(" ");
					for (String word : words) {
						if (!index.containsKey(word)) {
							index.put(word, new TreeSet<>());
						}
						index.get(word).add(filePath);
					}
				}
			} catch (FileNotFoundException ex) {
				LOGGER.error(ex.getMessage());
			}
		}
		return index;
	}

	private List<File> getSourceFiles() {
		List<File> sourceFilePath = null;
		try {
			sourceFilePath = getSourceFileContents();
		} catch (IOException e) {
			LOGGER.fatal("Unable to open the file '" + sourceFile + "'");
			LOGGER.error(e);
		}
		List<File> indexableFiles = new ArrayList<>();
		return getIndexableFiles(sourceFilePath, indexableFiles);
	}

	private List<File> getIndexableFiles(List<File> sourceFilePath, List<File> indexableFiles) {
		for (File file : sourceFilePath) {
			if (file.isFile()) {
				indexableFiles.add(file);
			} else {
				File[] filesArr = file.listFiles(new FilenameFilter() {
					@Override
					public boolean accept(File dir, String name) {
						File file = new File(dir, name);
						if (file.isFile()) {
							String extension = FilenameUtils.getExtension(file.getAbsolutePath());
							return extension.equalsIgnoreCase("txt");
						}
						return true;
					}
				});
				if (filesArr != null) {
					List<File> files = Arrays.asList(filesArr);
					getIndexableFiles(files, indexableFiles);
				}
			}
		}
		return indexableFiles;
	}

	private List<File> getSourceFileContents() throws IOException {
		List<File> sourceFilePath = new ArrayList<>();
		FileReader fr = new FileReader(sourceFile);
		BufferedReader br = new BufferedReader(fr);
		String str;
		while ((str = br.readLine()) != null) {
			sourceFilePath.add(new File(str));
		}
		br.close();
		return sourceFilePath;
	}
}
