package com.shrinivas.documentindexer.document;

import java.util.Set;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

@Document(collection = "index")
public class Index {

	@Id
	private String word;

	private Set<String> files;

	public Index() {
	}

	public Index(String word, Set<String> files) {
		super();
		this.word = word;
		this.files = files;
	}

	public String getWord() {
		return word;
	}

	public void setWord(String word) {
		this.word = word;
	}

	public Set<String> getFiles() {
		return files;
	}

	public void setFiles(Set<String> files) {
		this.files = files;
	}

	@Override
	public String toString() {
		return "Index [word=" + word + ", files=" + files + "]";
	}

}
