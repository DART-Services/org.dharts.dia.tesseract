DARTS Tesseract Wrapper
=======================

Provides a Java wrapper to the [Tesseract OCR engine] (http://code.google.com/p/tesseract-ocr/) 
engine currently under development by Google. This wrapper relies on Tess4J to manage the JNI 
interface to the underlying C++ implementation of Tesseract. The chief goals of this project 
are to provide a more object-oriented interface to Tesseract for those who need to use this 
tool in Java-based applications. 

This project is released under the Apache License 2.0. Please see the NOTICE and LICENSE files
in this directory for more detail.

This project is currently in an early alpha stage. That means that it works (sometimes, for
some things) but is under active development and may change at any time without notice. 
Notably, my near term goal is greatly simplify the API by separating the configuration data
from the actual analysis. In general, it should be possible to construct a configuration object
and then create as many page analyzers based on that configuration as is desired.    

This is currently being developed as part of the [Visual Page] (http://visualpage.org) 
project and will, for the foreseeable future, reflect the specific needs of that effort.

Installation
------------

In its current instantiation, I've checked this into GitHub as a full Eclipse project with all
of the associated preferences, classpath and project files along with the required JAR files.
This isn't the most flexible approach to maintaining an open source project but it has the 
advantage that you should be able to check this out into your eclipse workspace and run it as
is (with one additional step). In the future, we may set up an ant-based build system that 
doesn't tie the project to a particular IDE, but for now, this approach should work for most 
situations. 

In addition to the code provided in GitHub, you will also need to checkout the appropriate
language data file. You can find a variety of languages available at the main Tesseract
project site: (http://code.google.com/p/tesseract-ocr/downloads/list). The language data files 
will be in the format `tesseract-ocr-3.01.<lang>.tar.gz`. The data files for version 3.00 will
work fine as well. The English language data file can be  found [here] 
(http://tesseract-ocr.googlecode.com/files/tesseract-ocr-3.01.eng.tar.gz). 

Once you've downloaded and uncompressed the appropriate language data files, place them in 
tessdata. This location is configurable, so if you'd rather store these files elsewhere, there
are hooks for doing that.

The other components that you need (notably, the DLLs for Tesseract and Leptonica) are 
included in the source code repo on GitHub. These are currently located in the main project 
directory. In the future, these will move into a different folder and there will be hooks to 
configure the system to look them up from an abitrary location but, for now, loading the DLLs 
from the project directory provides an easy way to get things off the ground. 