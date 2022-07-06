package com.example.textlinear;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import io.paperdb.Paper;

public class PaperWords{

    public void addWords(WordsPainted word){
        int count = 0;
        ArrayList<WordsPainted> words = getWords();
        if (words != null){
            for (int i = 0; i < words.size(); i++){
                if (words.get(i).getStartIndex() == word.getStartIndex() || words.get(i).getEndIndex() == word.getEndIndex()) count++;
            }
            if (count == 0){
                words.add(word);
                saveWord(words);
            }
        }else{
            words.add(word);
            saveWord(words);
        }
    }

    public void deleteWords(WordsPainted word){
        ArrayList<WordsPainted> words = getWords();
        if (words != null){
            for (int i = 0; i < words.size();i++){
                if (words.get(i).getStartIndex() == word.getStartIndex() && words.get(i).getEndIndex() == word.getEndIndex()){
                    words.remove(words.get(i));
                    break;
                }
            }
            saveWord(words);
        }
    }



    public void saveWord(ArrayList<WordsPainted> words){
        Paper.book("words").write("words", words);
    }

    public ArrayList<WordsPainted> getWords(){
        return Paper.book("words").read("words", new ArrayList<>());
    }



}
