import os, sys
from nltk.tokenize import word_tokenize # Tokenizer

if __name__ == "__main__":
    # If you want to read from a file instead of passing data
    #text = open(sys.argv[1]).read()

    # read the first argument passed to script
    text = sys.argv[1]

    tokens = word_tokenize(text)
    print tokens