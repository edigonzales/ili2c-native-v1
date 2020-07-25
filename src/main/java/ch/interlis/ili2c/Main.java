
package ch.interlis.ili2c;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;

import ch.ehi.basics.logging.EhiLogger;
import ch.ehi.basics.view.GenericFileFilter;
import ch.interlis.ili2c.config.BoidEntry;
import ch.interlis.ili2c.config.Configuration;
import ch.interlis.ili2c.config.FileEntry;
import ch.interlis.ili2c.config.FileEntryKind;
import ch.interlis.ili2c.config.GenerateOutputKind;
import ch.interlis.ili2c.generator.TransformationParameter;
import ch.interlis.ili2c.generator.nls.Ili2TranslationXml;
import ch.interlis.ili2c.generator.nls.ModelElements;
import ch.interlis.ili2c.gui.UserSettings;
import ch.interlis.ili2c.metamodel.Element;
import ch.interlis.ili2c.metamodel.Ili2cMetaAttrs;
import ch.interlis.ili2c.metamodel.ObjectPath;
import ch.interlis.ili2c.metamodel.PathEl;
import ch.interlis.ili2c.metamodel.TransferDescription;
import ch.interlis.ili2c.metamodel.Viewable;
import ch.interlis.ili2c.parser.Ili1Parser;
import ch.interlis.ili2c.parser.Ili22Parser;
import ch.interlis.ili2c.parser.Ili23Parser;
import ch.interlis.ili2c.parser.Ili24Parser;
import ch.interlis.ilirepository.impl.RepositoryVisitor;


public class Main {

    public static final String APP_JAR = "ili2c.jar";
    /**
     * name of application as shown to user.
     */
    public static final String APP_NAME = "ili2c";
    @Deprecated
    public static final String ILI_DIR = UserSettings.ILI_DIR;
    @Deprecated
    public static final String JAR_DIR = UserSettings.JAR_DIR;
    @Deprecated
    public static final String JAR_MODELS = UserSettings.JAR_MODELS;
    @Deprecated
    public static final String ILI_REPOSITORY = UserSettings.ILI_REPOSITORY;
    @Deprecated
    public static final String ILIDIR_SEPARATOR = UserSettings.ILIDIR_SEPARATOR;
    @Deprecated
    public static final String MODELS_SEPARATOR = UserSettings.MODELS_SEPARATOR;
    @Deprecated
    public static final String DEFAULT_ILIDIRS = UserSettings.DEFAULT_ILIDIRS;

    protected static boolean hasArg(String v1, String v2, String[] args) {
	for (int i = 0; i < args.length; i++) {
	    if (args[i].equals(v1) || args[i].equals(v2))
		return true;
	}
	return false;
    }


    protected static void printVersion() {
	System.err.println("INTERLIS Compiler, Version " + TransferDescription.getVersion());
	System.err.println("  Distributed by the Coordination of Geographic Information");
	System.err.println("  and Geographic Information Systems Group (COSIG), CH-3084 Wabern");
	System.err.println("  Developed by Adasys AG, CH-8005 Zurich");
	System.err.println("  Maintained by Eisenhut Informatik AG, CH-3400 Burgdorf");
	System.err.println("  See http://www.interlis.ch for information about INTERLIS");
	System.err.println("  Parts of this program have been generated by ANTLR; see http://www.antlr.org");
	System.err.println("  This product includes software developed by the");
	System.err.println("  Apache Software Foundation (http://www.apache.org/).");
    }


    protected static void printDescription() {
	System.err.println("DESCRIPTION");
	System.err.println("  Parses and compiles INTERLIS Version 2.3 data model definitions.");
	System.err.println("  Other options include conversion from INTERLIS Version 1 and back");
	System.err.println("  (option -o1) and generation of an XML-Schema, released 2001 (option -oXSD).");
    }


    protected static void printUsage(String progName) {
	System.err.println("USAGE");
	System.err.println("  " + progName + " [Options] file1.ili file2.ili ...");
    }


    protected static void printExamples(String progName) {
	System.err.println("EXAMPLES");
	System.err.println();
	System.err.println("Check whether an INTERLIS definition in \"file1.ili\" is valid:");
	System.err.println("    java -jar " + progName + " file1.ili");
	System.err.println();
	System.err.println("Check whether a definition distributed over several files is valid:");
	System.err.println("    java -jar " + progName + " file1.ili file2.ili");
	System.err.println();
	System.err.println("Generate an INTERLIS-1 definition:");
	System.err.println("    java -jar " + progName + " -o1 file1.ili file2.ili");
	System.err.println();
	System.err.println("Generate an INTERLIS-2 definition:");
	System.err.println("    java -jar " + progName + " -o2 file1.ili file2.ili");
	System.err.println();
	System.err.println("Generate a definition of the predefined MODEL INTERLIS:");
	System.err.println("    java -jar " + progName + " -o2 --with-predefined");
	System.err.println();
	System.err.println("Generate an XML-Schema:");
	System.err.println("    java -jar " + progName + " -oXSD file1.ili file2.ili");
    System.err.println();
    System.err.println("Generate a Translation-XML file:");
    System.err.println("    java -jar " + progName + " -oNLS --out translation.xml file.ili");
    System.err.println();
    System.err.println("Generate a translated INTERLIS-2 definition with help of a Translation-XML file:");
    System.err.println("    java -jar " + progName + " -o2 --out file_it.ili --lang it --nlsxml translation.xml file.ili");
	System.err.println();
    System.err.println("Generate a CRS transformed INTERLIS-2 definition:");
    System.err.println("    java -jar " + progName + " -o2 --out file_LV95.ili --trafoNewModel model_LV95 --trafoDiff 2000000,1000000 --trafoFactor 1,1 --trafoEpsg 2056 --trafoImports  GeometryCHLV95_V1=GeometryCHLV03_V1 file.ili");
    System.err.println();
    System.err.println("List all models starting in the given repository:");
    System.err.println("    java -jar " + progName + " --listModels "+UserSettings.ILI_REPOSITORY);
    System.err.println();
    System.err.println("List all data starting in the given repository:");
    System.err.println("    java -jar " + progName + " --listData "+UserSettings.ILI_REPOSITORY);
    System.err.println();
    }


    public static void main(String[] args) {
	boolean emitPredefined = false;
	boolean doAuto = true;
	boolean checkMetaObjs = false;
	boolean doCheckRepoIlis = false;
	boolean doCloneRepos = false;
    boolean doListModels = false;
    boolean doListAllModels = false;
    boolean doListModels2 = false;
    boolean doListAllModels2 = false;
    boolean doListData = false;
	boolean withWarnings = true;
	int numErrorsWhileGenerating = 0;
	String notifyOnError = "compiler@interlis.ch";
	String ilidirs = UserSettings.DEFAULT_ILIDIRS;
	String httpProxyHost = null;
	String httpProxyPort = null;
	String translationDef=null;
    Ili2cMetaAttrs ili2cMetaAttrs=new Ili2cMetaAttrs();
    
    TransformationParameter params = new TransformationParameter();
    
	if (args.length == 0) {
	    ch.interlis.ili2c.gui.Main.main(args);
	    return;
	}

	if (hasArg("-u", "--usage", args)) {
	    printUsage(APP_NAME);
	    return;
	}

	if (hasArg("-h", "--help", args) || args.length == 0) {
	    printVersion();
	    System.err.println();
	    printDescription();
	    System.err.println();
	    printUsage(APP_NAME);
	    System.err.println();
	    System.err.println("OPTIONS");
	    System.err.println();
	    System.err.println("--no-auto             don't look automatically after required models.");
	    System.err.println("-o0                   Generate no output (default).");
	    System.err.println("-o1                   Generate INTERLIS-1 output.");
	    System.err.println("-o2                   Generate INTERLIS-2 output.");
	    System.err.println("-oXSD                 Generate an XTF XML-Schema.");
	    System.err.println("-oGML                 Generate an eCH-118-1.0/GML3.2-Schema.");
	    System.err.println("-oILIGML2             Generate an eCH-118-2.0/GML3.2-Schema.");
	    System.err.println("-oFMT                 Generate an INTERLIS-1 Format.");
	    System.err.println("-oIMD                 Generate Model as IlisMeta07 INTERLIS-Transfer (XTF).");
	    System.err.println("-oIMD16               Generate Model as IlisMeta16 INTERLIS-Transfer (XTF).");
	    System.err.println("-oNLS                 Generate an Translation-XML file.");
	    System.err.println("--nlsxml file         Name of the Translation-XML file.");
	    System.err.println("--lang lang           Language (de,fr,it or en).");
	    System.err.println("--trafoDiff d_x,d_y   offset to calculate new coord domains");
	    System.err.println("--trafoFactor f_x,f_y factor to calculate new coord domains");
	    System.err.println("--trafoEpsg code      new EPSG code (e.g. 2056)");
	    System.err.println("--trafoImports  newImport=oldImport change the name of an imported model (e.g. GeometryCHLV95_V1=GeometryCHLV03_V1)");
	    System.err.println("--trafoNewModel newName name of the new model.");
	    System.err.println("-oUML                 Generate Model as UML2/XMI-Transfer (eclipse flavour).");
	    System.err.println("-oIOM                 (deprecated) Generate Model as INTERLIS-Transfer (XTF).");
	    System.err.println("--check-repo-ilis uri   check all ili files in the given repository.");
	    System.err.println("--clone-repos         clones the given repositories to the --out folder.");
        System.err.println("--listModels uri      list all models starting in the given repository. (IliRepository09)");
        System.err.println("--listAllModels uri   list all models (without removing old entries) starting in the given repository. (IliRepository09)");
        System.err.println("--listModels2 uri     list all models starting in the given repository. (IliRepository20)");
        System.err.println("--listAllModels2 uri  list all models (without removing old entries) starting in the given repository. (IliRepository20)");
        System.err.println("--listData uri        list all data starting in the given repository.");
	    System.err.println("--translation translatedModel=originModel assigns a translated model to its orginal language equivalent.");
	    System.err.println("--out file/dir        file or folder for output (folder must exist).");
	    System.err.println("--modeldir " + ilidirs + " list of directories with ili-files.");
	    System.err.println("--proxy host          proxy server to access model repositories.");
	    System.err.println("--proxyPort port      proxy port to access model repositories.");
	    System.err.println("--with-predefined     Include the predefined MODEL INTERLIS in");
	    System.err.println("                      the output. Usually, this is omitted.");
	    System.err.println("--without-warnings    Report only errors, no warnings. Usually,");
	    System.err.println("                      warnings are generated as well.");
	    System.err.println("--trace               Display detailed trace messages.");
	    System.err.println("--quiet               Suppress info messages.");
	    System.err.println("-h|--help             Display this help text.");
	    System.err.println("-u|--usage            Display short information about usage.");
	    System.err.println("-v|--version          Display the version of " + APP_NAME + ".");
	    System.err.println();
	    printExamples(APP_NAME);
	    return;
	}

	if (hasArg("-v", "--version", args)) {
	    printVersion();
	    return;
	}

	try {
	    String outfile = null;
		String language = null;
		String nlsxmlFilename = null;
	    int outputKind = GenerateOutputKind.NOOUTPUT;

	    ArrayList ilifilev = new ArrayList();
	    for (int i = 0; i < args.length; i++) {

		if (args[i].equals("--with-predefined")) {
		    emitPredefined = true;
		    continue;
		}
		if (args[i].equals("--trace")) {
		    EhiLogger.getInstance().setTraceFilter(false);
		    continue;
		}
		if (args[i].equals("--quiet")) {
		    ch.ehi.basics.logging.StdListener.getInstance().skipInfo(true);
		    continue;
		}
		if (args[i].equals("--no-auto")) {
		    doAuto = false;
		    continue;
		}
		if (args[i].equals("--check-repo-ilis")) {
		    doCheckRepoIlis = true;
		    continue;
		}
		if (args[i].equals("--clone-repos")) {
		    doCloneRepos = true;
		    continue;
		}
        if (args[i].equals("--listModels")) {
            doListModels = true;
            continue;
        }
        if (args[i].equals("--listModels2")) {
            doListModels2 = true;
            continue;
        }
        if (args[i].equals("--listData")) {
            doListData = true;
            continue;
        }
        if (args[i].equals("--listAllModels")) {
            doListAllModels = true;
            continue;
        }
        if (args[i].equals("--listAllModels2")) {
            doListAllModels2 = true;
            continue;
        }
		if (args[i].equals("--out")) {
		    i++;
		    outfile = args[i];
		    continue;
		}
		if (args[i].equals("--translation")) {
		    i++;
            String modelNameMappings[]=args[i].split(";");
            for(String modelNameMapping:modelNameMappings) {
                String modelNames[]=modelNameMapping.split("=");
                String translatedModelName=modelNames[0];
                String originLanguageModelName=modelNames[1];
                if(translatedModelName!=null && originLanguageModelName!=null){
                    ili2cMetaAttrs.setMetaAttrValue(translatedModelName, Ili2cMetaAttrs.ILI2C_TRANSLATION_OF, originLanguageModelName);
	            }
            }
		    continue;
		}
		if (args[i].equals("--proxy")) {
		    i++;
		    httpProxyHost = args[i];
		    continue;
		}
		if (args[i].equals("--proxyPort")) {
		    i++;
		    httpProxyPort = args[i];
		    continue;
		}
		if (args[i].equals("--lang")) {
			i++;
			language = args[i];
			continue;
		}
		if (args[i].equals("--trafoDiff")) {
			i++;
			String[] diffs = args[i].split("\\,");
			params.setDiff_x(Double.parseDouble(diffs[0]));
			params.setDiff_y(Double.parseDouble(diffs[1]));
		    continue;
		}
		if (args[i].equals("--trafoFactor")) {
			i++;
			String[] factor = args[i].split("\\,");
			params.setFactor_x(Double.parseDouble(factor[0]));
			params.setFactor_y(Double.parseDouble(factor[1]));
		    continue;
		}
		if (args[i].equals("--trafoEpsg")) {
			i++;
			params.setEpsgCode(Integer.parseInt(args[i]));
		    continue;
		}
		if (args[i].equals("--trafoImports")) {
			i++;
			String[] imports = args[i].split("\\=");
			params.setImportModels(new TransformationParameter.ModelTransformation[] {
					new TransformationParameter.ModelTransformation(imports[0], imports[1])
			});
		    continue;
		}
		if (args[i].equals("--trafoNewModel")) {
			i++;
			params.setNewModelName(args[i]);
		    continue;
		}
		if (args[i].equals("--nlsxml")) {
			i++;
			nlsxmlFilename = args[i];
			continue;
		}
		if (args[i].equals("--ilidirs") || args[i].equals("--modeldir")) {
		    i++;
		    ilidirs = args[i];
		    continue;
		} else if (args[i].equals("-o0")) {
		    outputKind = GenerateOutputKind.NOOUTPUT;
		    continue;
		} else if (args[i].equals("-o1")) {
		    outputKind = GenerateOutputKind.ILI1;
		    continue;
		} else if (args[i].equals("-o2")) {
		    outputKind = GenerateOutputKind.ILI2;
		    continue;
		} else if (args[i].equals("-oXSD")) {
		    outputKind = GenerateOutputKind.XMLSCHEMA;
		    continue;
		} else if (args[i].equals("-oFMT")) {
		    outputKind = GenerateOutputKind.ILI1FMTDESC;
		    continue;
		} else if (args[i].equals("-oGML")) {
		    outputKind = GenerateOutputKind.GML32;
		    continue;
		} else if (args[i].equals("-oILIGML2")) {
		    outputKind = GenerateOutputKind.ILIGML2;
		    continue;
		} else if (args[i].equals("-oETF1")) {
		    outputKind = GenerateOutputKind.ETF1;
		    continue;
		} else if (args[i].equals("-oIMD")) {
		    outputKind = GenerateOutputKind.IMD;
		    continue;
		} else if (args[i].equals("-oIMD16")) {
		    outputKind = GenerateOutputKind.IMD16;
		    continue;
		} else if (args[i].equals("-oUML")){
			outputKind=GenerateOutputKind.UML21;
			continue;
		} else if (args[i].equals("-oIOM")) {
		    outputKind = GenerateOutputKind.IOM;
		    continue;
		} else if (args[i].equals("-oNLS")) {
			outputKind = GenerateOutputKind.XMLNLS;
			continue;
		} else if (args[i].equals("--without-warnings")) {
		    withWarnings = false;
		    continue;
		} else if (args[i].equals("--with-warnings")) {
		    withWarnings = true;
		    continue;
		} else if (args[i].charAt(0) == '-') {
		    System.err.println(APP_NAME + ":Unknown option: " + args[i]);
		    continue;
		} else {
			String filename = args[i];
			if (doCheckRepoIlis  || doCloneRepos 
			        || doListModels || doListAllModels 
                    || doListModels2 || doListAllModels2 
			        || doListData || new File(filename).isFile()) {
				ilifilev.add(filename);
			} else {
				EhiLogger.logError(args[i] + ": There is no such file.");
			}
		}

	    }

	    UserSettings settings = new UserSettings();
	    setDefaultIli2cPathMap(settings);
	    settings.setHttpProxyHost(httpProxyHost);
	    settings.setHttpProxyPort(httpProxyPort);
	    settings.setIlidirs(ilidirs);
	    Configuration config = new Configuration();
	    Iterator ilifilei = ilifilev.iterator();
	    while (ilifilei.hasNext()) {
		String ilifile = (String) ilifilei.next();
		FileEntry file = new FileEntry(ilifile, FileEntryKind.ILIMODELFILE);
		config.addFileEntry(file);
	    }
	    if (doAuto) {
	    	config.setAutoCompleteModelList(true);
	    } else {
	    	config.setAutoCompleteModelList(false);
	    }
	    config.setGenerateWarnings(withWarnings);
	    config.setOutputKind(outputKind);
		config.setLanguage(language);
		config.setNlsxmlFilename(nlsxmlFilename);
		config.setParams(params);
	    if (doCloneRepos 
	            || doListModels || doListAllModels 
                || doListModels2 || doListAllModels2 
	            || doListData || outputKind != GenerateOutputKind.NOOUTPUT) {
			if (outfile != null) {
			    config.setOutputFile(outfile);
			} else {
			    config.setOutputFile("-");
			}
	    }

		EhiLogger.logState(APP_NAME+"-"+TransferDescription.getVersion());
			if (doCheckRepoIlis) {
				boolean failed = new CheckReposIlis().checkRepoIlis(config, settings);
				if (failed) {
					EhiLogger.logError("check of ili's in repositories failed");
					System.exit(1);
				}
			}else if (doCloneRepos) {
					boolean failed = new CloneRepos().cloneRepos(config, settings);
					if (failed) {
						EhiLogger.logError("clone of repositories failed");
						System.exit(1);
					}
            }else if (doListModels || doListAllModels) {
                boolean failed = new ListModels().listModels(config, settings,doListModels==true);
                if (failed) {
                    EhiLogger.logError("list of models failed");
                    System.exit(1);
                }
            }else if (doListModels2 || doListAllModels2) {
                boolean failed = new ListModels2().listModels(config, settings,doListModels2==true);
                if (failed) {
                    EhiLogger.logError("list of models failed");
                    System.exit(1);
                }
            }else if (doListData) {
                boolean failed = new ListData().listData(config, settings);
                if (failed) {
                    EhiLogger.logError("list of data failed");
                    System.exit(1);
                }
			} else {
				// compile models
				TransferDescription td = runCompiler(config, settings,ili2cMetaAttrs);
				if (td == null) {
					EhiLogger.logError("compiler failed");
					System.exit(1);
				}
			}
	} catch (Exception ex) {
	    EhiLogger.logError(APP_NAME + ": An internal error has occured. Please notify " + notifyOnError, ex);
	    System.exit(1);
	}
    }




	public static ArrayList<String> getIliLookupPaths(ArrayList<String> ilifilev) {
	ArrayList<String> ilipathv = new ArrayList<String>();
	HashSet<String> seenDirs = new HashSet<String>();
	Iterator<String> ilifilei = ilifilev.iterator();

	while (ilifilei.hasNext()) {
	    String ilifile = ilifilei.next();
	    String parentdir = new File(ilifile).getAbsoluteFile().getParent();

	    if (!seenDirs.contains(parentdir)) {
		seenDirs.add(parentdir);
		ilipathv.add(parentdir);
	    }
	}
	String ili2cHome = getIli2cHome();
	if (ili2cHome != null) {
	    ilipathv.add(ili2cHome + File.separator + UserSettings.JAR_MODELS);
	}
	return ilipathv;
    }


    /**
     * Get the path of the "ili2c.jar" file.
     *
     * @return The JAR file path.
     */
    static public String getIli2cHome() {
	String classpath = System.getProperty("java.class.path");
	int index = classpath.toLowerCase().indexOf(APP_JAR);
	int start = classpath.lastIndexOf(File.pathSeparator, index) + 1;

	return (index > start) ? classpath.substring(start, index - 1) : null;
    }


    static public TransferDescription runCompiler(Configuration config) {
	return runCompiler(config, null);
    }


    static public void setDefaultIli2cPathMap(ch.ehi.basics.settings.Settings settings) {
	HashMap<String, String> pathmap = new HashMap<String, String>();

	String ili2cHome = getIli2cHome();
	if (ili2cHome != null) {
		pathmap.put(UserSettings.JAR_DIR, ili2cHome + File.separator + UserSettings.JAR_MODELS);
	}
	pathmap.put(UserSettings.ILI_DIR, null);
	settings.setTransientObject(UserSettings.ILIDIRS_PATHMAP, pathmap);
    }


    static public TransferDescription runCompiler(Configuration config, ch.ehi.basics.settings.Settings settings) {
    	return runCompiler(config,settings,null);
    }
    static public TransferDescription runCompiler(Configuration config, ch.ehi.basics.settings.Settings settings,Ili2cMetaAttrs metaAttrs) {
	ArrayList<FileEntry> filev = new ArrayList<FileEntry>();
	boolean doAutoCompleteModelList = config.isAutoCompleteModelList();

    if (doAutoCompleteModelList) {
        if (settings != null) {
            String ilidirs = settings.getValue(UserSettings.ILIDIRS);
            if (ilidirs == null) {
                doAutoCompleteModelList = false;
            }
        }
    }
    if (doAutoCompleteModelList) {
	    if (settings == null) {
    		ArrayList ilifilev = new ArrayList();
    		Iterator filei = config.iteratorFileEntry();
    		while (filei.hasNext()) {
    		    FileEntry e = (FileEntry) filei.next();
    		    if (e.getKind() == FileEntryKind.ILIMODELFILE) {
    			String fileName = e.getFilename();
    			ilifilev.add(fileName);
    		    }
    		}
    		ArrayList modeldirv = getIliLookupPaths(ilifilev);
    		ch.interlis.ili2c.config.Configuration files;
    		try {
    		    files = ModelScan.getConfigWithFiles(modeldirv, ilifilev);
    		} catch (Ili2cException ex) {
    		    EhiLogger.logError("ili-file scan failed", ex);
    		    return null;
    		}
    		if (files == null) {
    		    EhiLogger.logError("ili-file scan failed");
    		    return null;
    		}
    		logIliFiles(files);
    		// copy result of scan to original config
    		filei = files.iteratorFileEntry();
    		while (filei.hasNext()) {
    		    FileEntry e = (FileEntry) filei.next();
    		    filev.add(e);
    		}
	    } else {
	        String iliVersion=settings.getValue(UserSettings.ILI_LANGUAGE_VERSION);
            double version=0.0;
            if(iliVersion!=null) {
                version=Double.parseDouble(iliVersion);
            }

    		ArrayList<String> ilifilev = new ArrayList<String>();
    
    		for (Iterator filei = config.iteratorFileEntry(); filei.hasNext();) {
    		    FileEntry e = (FileEntry) filei.next();
    
    		    if (e.getKind() == FileEntryKind.ILIMODELFILE) {
    		        ilifilev.add(e.getFilename());
    		    }
    		}

    		setHttpProxySystemProperties(settings);
    		
    		HashMap pathmap = (HashMap) settings.getTransientObject(UserSettings.ILIDIRS_PATHMAP);
    
    		ArrayList modeldirv = getModelRepos(settings, ilifilev, pathmap);
    
    		// get/create repository manager
    		ch.interlis.ilirepository.IliManager manager = (ch.interlis.ilirepository.IliManager) settings
                    .getTransientObject(UserSettings.CUSTOM_ILI_MANAGER);
    		if(manager==null) {
    		    manager=new ch.interlis.ilirepository.IliManager();
    		}
    		ch.interlis.ilirepository.IliResolver resolver = (ch.interlis.ilirepository.IliResolver) settings
    		        .getTransientObject(UserSettings.CUSTOM_ILI_RESOLVER);
    		if (resolver != null) {
    		    manager.setResolver(resolver);
    		}
    		// set list of repositories to search
    		manager.setRepositories((String[]) modeldirv.toArray(new String[1]));
    		String tempReposUri=settings.getTransientValue(UserSettings.TEMP_REPOS_URI);
    		if(tempReposUri!=null) {
    	        manager.setIliFiles(RepositoryVisitor.fixUri(tempReposUri),
    	                (ch.interlis.ilirepository.IliFiles) settings
    	                        .getTransientObject(UserSettings.TEMP_REPOS_ILIFILES));
    		}

    		// get complete list of required ili-files
    		try {
    		    Configuration fileconfig = manager.getConfigWithFiles(ilifilev,metaAttrs,version);
    		    ch.interlis.ili2c.Ili2c.logIliFiles(fileconfig);
    		    Iterator filei = fileconfig.iteratorFileEntry();
    		    while (filei.hasNext()) {
        			FileEntry e = (FileEntry) filei.next();
        			filev.add(e);
    		    }
    		} catch (Ili2cException ex) {
    		    EhiLogger.logError(ex);
    		    return null;
    		}
	    }
	} else {
	    Iterator filei = config.iteratorFileEntry();
	    while (filei.hasNext()) {
    		FileEntry e = (FileEntry) filei.next();
    		filev.add(e);
	    }
	}
	TransferDescription desc = new TransferDescription();
	boolean emitPredefined = config.isIncPredefModel();
	boolean checkMetaObjs = config.isCheckMetaObjs();
	CompilerLogEvent.enableWarnings(config.isGenerateWarnings());

	// boid to basket mappings
	Iterator boidi = config.iteratorBoidEntry();
	while (boidi.hasNext()) {
	    BoidEntry e = (BoidEntry) boidi.next();
	    desc.addMetadataMapping(e.getMetaDataUseDef(), e.getBoid());
	}

	// model and metadata files
	double version = 0.0;
	Iterator filei = filev.iterator();
	while (filei.hasNext()) {
	    FileEntry e = (FileEntry) filei.next();
	    if (e.getKind() == FileEntryKind.METADATAFILE) {
		if (checkMetaObjs) {
		    /* Don't continue if there is a fatal error. */
		    if (!ch.interlis.ili2c.parser.MetaObjectParser.parse(desc, e.getFilename())) {
			return null;
		    }
		}
	    } else {
		String streamName = e.getFilename();
		if (version == 0.0) {
		    version = ModelScan.getIliFileVersion(new File(streamName));
		}
		java.io.Reader stream=null;
		try {
            stream = new java.io.InputStreamReader(new FileInputStream(streamName),"UTF-8");
		} catch (Exception ex) {
		    EhiLogger.logError(ex);
		    return null;
		}
		ch.ehi.basics.logging.ErrorTracker tracker = null;
		try {
		    tracker = new ch.ehi.basics.logging.ErrorTracker();
		    EhiLogger.getInstance().addListener(tracker);
					if (version == 2.2) {
						if (!Ili22Parser.parseIliFile(desc, streamName, stream,
								checkMetaObjs, 0,metaAttrs)) {
							return null;
						}
					}else if(version==2.4){
						if (!Ili24Parser.parseIliFile(desc, streamName, stream,
								checkMetaObjs, 0,metaAttrs)) {
							return null;
						}
					} else if (version == 1.0) {
						if (!Ili1Parser.parseIliFile(desc, streamName, stream,
								0,metaAttrs)) {
							return null;
						}
                    } else if (version == 2.3) {
                        if (!Ili23Parser.parseIliFile(desc, streamName, stream,
                                checkMetaObjs, 0,metaAttrs)) {
                            return null;
                        }
					} else {
					    EhiLogger.logError(Element.formatMessage("err_wrongInterlisVersion",Double.toString(version)));
					}
		    if (tracker.hasSeenErrors()) {
			return null;
		    }
		} catch (java.lang.Exception ex) {
		    EhiLogger.logError(ex);
		    return null;
		} finally {
		    if (tracker != null) {
			EhiLogger.getInstance().removeListener(tracker);
			tracker = null;
		    }
		    try {
			stream.close();
		    } catch (java.io.IOException ex) {
			EhiLogger.logError(ex);
		    }
		}
	    }
	}

	java.io.Writer out = null;
	try{
		// output options
		switch (config.getOutputKind()) {
		case GenerateOutputKind.NOOUTPUT:
		    break;
		case GenerateOutputKind.ILI1:
		    if ("-".equals(config.getOutputFile())) {
			out = new BufferedWriter(new OutputStreamWriter(System.out));
			;
		    } else {
			try {
			    out = new java.io.OutputStreamWriter(new FileOutputStream(config.getOutputFile()),"UTF-8");
			} catch (IOException ex) {
			    EhiLogger.logError(ex);
			    return desc;
			}
		    }
		    if (config.getParams() != null) {
		    	ch.interlis.ili2c.generator.Interlis1Generator.generate(out, desc, config.getParams());
		    } else {
		    	ch.interlis.ili2c.generator.Interlis1Generator.generate(out, desc, null);
		    }
		    break;
		case GenerateOutputKind.ILI2:
		    if ("-".equals(config.getOutputFile())) {
			out = new BufferedWriter(new OutputStreamWriter(System.out));
			;
		    } else {
			try {
                out = new java.io.OutputStreamWriter(new FileOutputStream(config.getOutputFile()),"UTF-8");
			} catch (IOException ex) {
			    EhiLogger.logError(ex);
			    return desc;
			}
		    }
		    ch.interlis.ili2c.generator.Interlis2Generator gen = new ch.interlis.ili2c.generator.Interlis2Generator();
			if (config.getLanguage() != null && config.getNlsxmlFilename() != null) {
				ModelElements modelElements = Ili2TranslationXml
						.readModelElementsXml(new File(config.getNlsxmlFilename()));
				gen.generateWithNewLanguage(out, desc, modelElements, config.getLanguage());
			} else if (config.getParams() != null) { 
				gen.generateWithNewCrs(out, desc, config.getParams());
			}else {
				gen.generate(out, desc, emitPredefined);
			}
		    break;
		case GenerateOutputKind.XMLSCHEMA:
		{
			String ver=desc.getLastModel().getIliVersion();
		    if (ver.equals("2.2") || ver.equals("2.3")) {
			    if ("-".equals(config.getOutputFile())) {
					out = new BufferedWriter(new OutputStreamWriter(System.out));
					;
				    } else {
					try {
					    out = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(config.getOutputFile()),
						    "UTF-8"));
					} catch (IOException ex) {
					    EhiLogger.logError(ex);
					    return desc;
					}
				    }
				    if (ver.equals("2.2")) {
				    	ch.interlis.ili2c.generator.XSD22Generator.generate(out, desc);
				    } else if (ver.equals("2.3")){
				    	ch.interlis.ili2c.generator.XSDGenerator.generate(out, desc);
				    }
		    	
		    }else{
			    ch.interlis.ili2c.generator.XSD24Generator.generate (desc, new java.io.File(config.getOutputFile()));
		    }
			
		}
		    break;
		case GenerateOutputKind.ILI1FMTDESC:
		    if ("-".equals(config.getOutputFile())) {
			out = new BufferedWriter(new OutputStreamWriter(System.out));
			;
		    } else {
			try {
                out = new java.io.OutputStreamWriter(new FileOutputStream(config.getOutputFile()),"UTF-8");
			} catch (IOException ex) {
			    EhiLogger.logError(ex);
			    return desc;
			}
		    }
		    ch.interlis.ili2c.generator.Interlis1Generator.generateFmt(out, desc);
		    break;
		case GenerateOutputKind.GML32:
		    ch.interlis.ili2c.generator.Gml32Generator.generate(desc, config.getOutputFile());
		    break;
		case GenerateOutputKind.ILIGML2:
		    ch.interlis.ili2c.generator.Iligml20Generator.generate(desc, config.getOutputFile());
		    break;
		case GenerateOutputKind.ETF1:
		    ch.interlis.ili2c.generator.ETF1Generator.generate(desc, config.getOutputFile());
		    break;
		case GenerateOutputKind.IMD:
		    ch.interlis.ili2c.generator.ImdGenerator.generate(new java.io.File(config.getOutputFile()), desc, APP_NAME +
			    "-" + TransferDescription.getVersion());
		    break;
		case GenerateOutputKind.XMLNLS:
			generateXML(config, desc);
			break;
		case GenerateOutputKind.IMD16:
		    ch.interlis.ili2c.generator.Imd16Generator.generate(new java.io.File(config.getOutputFile()), desc, APP_NAME +
			    "-" + TransferDescription.getVersion());
		    break;
		case GenerateOutputKind.UML21:
			  ch.interlis.ili2c.generator.Uml21Generator.generate(new java.io.File(config.getOutputFile()),desc);
			  break;
		case GenerateOutputKind.IOM:
		    if ("-".equals(config.getOutputFile())) {
			out = new BufferedWriter(new OutputStreamWriter(System.out));
			;
		    } else {
			try {
			    out = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(config.getOutputFile()),
				    "UTF-8"));
			} catch (IOException ex) {
			    EhiLogger.logError(ex);
			    return desc;
			}
		    }
		    ch.interlis.ili2c.generator.iom.IomGenerator.generate(out, desc);
		    break;
		default:
		    // ignore
		    break;
		}
		
	} catch (Throwable e) {
		EhiLogger.logError("failed to generate output",e);
		return null;
	}finally{
		if (out != null) {
		    try {
			out.close();
		    } catch (java.io.IOException ex) {
			EhiLogger.logError(ex);
		    }
		}
	}
	return desc;
    }

	private static void generateXML(Configuration config, TransferDescription desc) throws Exception{
		FileEntry e = (FileEntry) config.getFileEntry(config.getSizeFileEntry() - 1);
		Ili2TranslationXml xml = new Ili2TranslationXml();
		ModelElements eles=xml.convertTransferDescription2ModelElements(desc);
		Ili2TranslationXml.writeModelElementsAsXML(eles,new File(config.getOutputFile()));
	}

	private static ArrayList getModelRepos(
			ch.ehi.basics.settings.Settings settings,
			ArrayList<String> ilifilev, HashMap pathmap) {
		ArrayList modeldirv;
		modeldirv=new ArrayList();
		if (pathmap == null) {
		    pathmap = new HashMap();
		}

		String ilidirs = settings.getValue(UserSettings.ILIDIRS);
		String modeldirs[] = ilidirs.split(UserSettings.ILIDIR_SEPARATOR);
		HashSet ilifiledirs = new HashSet();

		for (int modeli = 0; modeli < modeldirs.length; modeli++) {
			String m = modeldirs[modeli];

			if (m.equals(UserSettings.ILI_DIR) && pathmap.containsKey(UserSettings.ILI_DIR)) {
				for (int filei = 0; filei < ilifilev.size(); filei++) {
					String ilifile = ilifilev.get(filei);

					if (GenericFileFilter.getFileExtension(ilifile) != null) {
						m = new java.io.File(ilifile).getAbsoluteFile()
								.getParentFile().getAbsolutePath();
						if (m != null && m.length() > 0) {
							if (!ilifiledirs.contains(m)) {
								ilifiledirs.add(m);
								modeldirv.add(m);
							}
						}
					}
				}
			} else if (m.startsWith("%")) {
				String key = m;
				EhiLogger.traceState("pathmap key <"+key+">");
				if (pathmap.containsKey(key)) {
					m = (String) pathmap.get(key);
					if (m != null && m.length() > 0) {
						modeldirv.add(m);
					}
				}
			} else {
				if (m != null && m.length() > 0) {
					modeldirv.add(m);
				}
			}
		}
		return modeldirv;
	}


	public static void setHttpProxySystemProperties(
			ch.ehi.basics.settings.Settings settings) {
		String httpProxyHost = settings.getValue(UserSettings.HTTP_PROXY_HOST);
		String httpProxyPort = settings.getValue(UserSettings.HTTP_PROXY_PORT);

		if (httpProxyHost != null) {
		    EhiLogger.logState("httpProxyHost <" + httpProxyHost + ">");
		    System.setProperty("http.proxyHost", httpProxyHost);
            System.setProperty("https.proxyHost", httpProxyHost);

		    if (httpProxyPort != null) {
			EhiLogger.logState("httpProxyPort <" + httpProxyPort + ">");
			System.setProperty("http.proxyPort", httpProxyPort);
            System.setProperty("https.proxyPort", httpProxyPort);
		    }
		} else {
		    System.setProperty("java.net.useSystemProxies", "true");
		}
	}


    static public boolean editConfig(Configuration config) {
	ch.interlis.ili2c.gui.Main dialog = new ch.interlis.ili2c.gui.Main();
	return dialog.showDialog();
    }

    @Deprecated
    public static String getVersion() {
	return TransferDescription.getVersion();
    }


    /**
     * compiles a set of ili models.
     */
    static public TransferDescription compileIliModels(ArrayList modelv, ArrayList modeldirv, String ilxFile) {
	Configuration config = null;

	try {
	    config = ModelScan.getConfig(modeldirv, modelv);
	} catch (Ili2cException ex) {
	    EhiLogger.logError("ili-file scan failed", ex);
	    return null;
	}
	if (config == null) {
	    return null;
	}
	config.setGenerateWarnings(false);
	logIliFiles(config);
	if (ilxFile != null) {
	    config.setOutputKind(GenerateOutputKind.IOM);
	    config.setOutputFile(ilxFile);
	} else {
	    config.setOutputKind(GenerateOutputKind.NOOUTPUT);
	}
	TransferDescription ret = runCompiler(config);
	return ret;
    }


    /**
     * compiles a set of ili files.
     */
    static public TransferDescription compileIliFiles(ArrayList filev, ArrayList modeldirv, String ilxFile) {
	Configuration config = null;

	try {
	    config = ModelScan.getConfigWithFiles(modeldirv, filev);
	} catch (Ili2cException ex) {
	    EhiLogger.logError("ili-file scan failed", ex);
	    return null;
	}
	if (config == null) {
	    return null;
	}
	logIliFiles(config);
	config.setGenerateWarnings(false);
	if (ilxFile != null) {
	    config.setOutputKind(GenerateOutputKind.IOM);
	    config.setOutputFile(ilxFile);
	} else {
	    config.setOutputKind(GenerateOutputKind.NOOUTPUT);
	}
	TransferDescription ret = runCompiler(config);
	return ret;
    }

    static public ObjectPath parseObjectOrAttributePath(Viewable viewable,String objectPath) throws Ili2cException
    {
        TransferDescription td=(TransferDescription) viewable.getContainer(TransferDescription.class);
        return Ili23Parser.parseObjectOrAttributePath(td,viewable, objectPath);
    }

    static public void logIliFiles(ch.interlis.ili2c.config.Configuration config) {
	java.util.Iterator filei = config.iteratorFileEntry();
	while (filei.hasNext()) {
	    ch.interlis.ili2c.config.FileEntry file = (ch.interlis.ili2c.config.FileEntry) filei.next();
	    EhiLogger.logState("ilifile <" + file.getFilename() + ">");
	}
    }
}
