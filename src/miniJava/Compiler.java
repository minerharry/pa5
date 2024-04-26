package miniJava;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

import miniJava.SyntacticAnalyzer.Parser;
import miniJava.SyntacticAnalyzer.Scanner;
import miniJava.SyntacticAnalyzer.Token;
import miniJava.SyntacticAnalyzer.TokenType;
import miniJava.AbstractSyntaxTrees.*;
import miniJava.AbstractSyntaxTrees.Package;
import miniJava.CodeGeneration.CodeGenerator;
import miniJava.ContextualAnalysis.ScopedIdentifier;
import miniJava.ContextualAnalysis.TypeChecker;
import miniJava.ContextualAnalysis.ScopedIdentifier.IdentificationError;
import miniJava.ContextualAnalysis.TypeChecker.TypeError;

public class Compiler {

	public static enum CompileLevel{
		SCAN(1),
		PARSE(2),
		IDENTIFY(3),
		TYPECHECK(4),
		GENERATE(5);
		CompileLevel(int l){
			level = l;
		}
		public int level;
		public boolean in(CompileLevel other){
			return other.level <= this.level;
		}
	}

	public static boolean IS_MINI = true;
	public int a,b;

	public static List<File> recursePaths(File start, String ext){
		ArrayList<File> out = new ArrayList<>();
		if (start.isDirectory()){
			for (File sub : start.listFiles()){
				if (sub.isFile()){
					if (sub.getName().endsWith(ext)) {
						out.add(sub);
					}
				}
				if (sub.isDirectory()){
					out.addAll(recursePaths(sub, ext));
				}
			}
		} else {
			if (start.getName().endsWith(ext)) out.add(start);
		}
		return out;
	}


	// Main function, the file to compile will be an argument.
	public static void main(String[] args) throws FileNotFoundException, IOException {

		boolean recurse = false;
		for (String arg : args){
			if (arg.equals("--recurse") || arg.equals("-r")){
				recurse = true;
				break;
			}
		}

		// TODO: Check to make sure a file path is given in args
		List<File> files = new ArrayList<>();
		for (String arg : args){
			if (arg.startsWith("-")){
				continue;
			}
			File f1 = new File(arg);
			files = recursePaths(f1,".java");
		}
		
		boolean verbose = false;
		boolean doContinue = false;
		for (String arg : args){
			if (arg.equals("--quiet") || arg.equals("-q")){
				verbose = false;
			}
			if (arg.equals("--verbose") || arg.equals("-v")){
				verbose = true;
			}
			if (arg.equals("--continue") || arg.equals("-c")){
				doContinue = true;
			}
		}
		if (verbose) System.out.println("Verbose: " + verbose);
		CompileLevel level = CompileLevel.GENERATE;

		
		java.util.Scanner inputScanner = new java.util.Scanner(System.in);
		
		try{
			boolean any_fail = false;

			Token.fullString = true;

			for (File f : files) {
				if (!f.getName().endsWith(".java")) continue;
				if (verbose){
					System.out.println("Compiling File: " + f.toPath().getFileName());
				}
				ErrorReporter reporter = new ErrorReporter();

				// TODO: Create the inputStream using new FileInputStream
				InputStream inputStream = new FileInputStream(f);
				
				// TODO: Instantiate the scanner with the input stream and error object
				Scanner scan = new Scanner(inputStream,reporter,f.getPath());
				
				if (!level.in(CompileLevel.PARSE)){
					while (true){
						scan.scan();
					}
				}
				
				// TODO: Instantiate the parser with the scanner and error object
				Parser parser = new Parser(scan, reporter);
				
				// TODO: Call the parser's parse function
				Package out = parser.parse();
				ASTDisplay disp = new ASTDisplay();
				
				if (!reporter.hasErrors() && level.in(CompileLevel.IDENTIFY)){
					// System.out.println("identifying");
					try{
						ScopedIdentifier id = new ScopedIdentifier();
						id.identifyProgram(out);
					} catch (IdentificationError e){
						reporter.reportError(e);
					}
				}

				if (!reporter.hasErrors() && level.in(CompileLevel.TYPECHECK)){
					// System.out.println("typing");
					try {
						TypeChecker tc = new TypeChecker();
						tc.typeCheck(out);
					} catch (TypeError e){
						reporter.reportError(e);
					}
				}

				if (!reporter.hasErrors() && level.in(CompileLevel.GENERATE)){
					String filename = "a.out";
					if (verbose){
						filename = "outs/elf/" + f.toPath().getFileName() + ".o";
					}
					// System.out.println("generating code!");
					try {
						CodeGenerator tc = new CodeGenerator(reporter);
						tc.parse((Package)out,filename,verbose);
					} catch (CompilerError e){
						reporter.reportError(e);
					}
				}

				// TODO: Check if any errors exist, if so, println("Error")
				//  then output the errors
				if (reporter.hasErrors()){
					System.out.println("Error");
					if (verbose) reporter.outputErrors(true);
					any_fail = true;
					// break;
				} else {
					System.out.println("Success");
					if (verbose) disp.showTree(out);
				}
					
				boolean shouldFail = f.getName().contains("fail");
				if (shouldFail != reporter.hasErrors()){
					any_fail = true;
					// System.out.println(shouldFail);
					// System.out.println(reporter.hasErrors());
					// reporter.outputErrors(true);
					System.out.println("Compiler mismatch: incorrect result for file " + f.getName());
					break;
				}
			

				

				if (!reporter.hasErrors()){
					PrintStream stdout = System.out;
					File testOut = new File("outs/ast/" + f.toPath().getFileName() + ".out");
					Path testPath = testOut.toPath();

					System.setOut(new PrintStream(testOut));
					disp.showTree(out);
					System.setOut(stdout);

					// FileInputStream testIn = new FileInputStream(testOut);
					// FileInputStream targetIn = new FileInputStream(f.getPath() + ".out");
					
					
					Path targetPath = Paths.get(f.getPath() + ".out");

					if (Files.exists(targetPath)){
						//exists an intended output (gradescope test)	
						BufferedReader buffTest = Files.newBufferedReader(testPath);
						BufferedReader buffTarget = Files.newBufferedReader(targetPath);
						
						boolean err = false;

						long lineNumber = 1;
						String line1 = "";
						String line2 = "";
						while ((line1 = buffTest.readLine()) != null) {
							line2 = buffTarget.readLine();
							if (line2 == null){
								System.out.println("Ran out of lines; Program result " + testPath + " longer than exprected result " + targetPath);
								err = true;
								break;
							} else if (!line1.equals(line2)) {
								System.out.println("Output mismatch: Program result at " + testPath + ":" + lineNumber + ":\n" +
								line1 + "\nDoes not match target at " + targetPath + ":" + lineNumber + ":\n" + line2);
								err = true;
								break;
							}
							lineNumber++;
						}
						if (buffTarget.readLine() != null && buffTest.readLine() == null) {
							System.out.println("Ran out of lines; Program result " + testPath + " shorter than expected result " + targetPath);
							err = true;
						}
					}
				}

				if (verbose){
					System.out.println("File " + f.toPath().getFileName() + " compiled successfully");
					System.out.println("---------------------------");
					if (!doContinue) inputScanner.nextLine();
				}
				
			}
			
			if (!any_fail && files.size() > 1){
				System.out.println("All tests passed");
			}
		} finally {
			inputScanner.close();
		}

		
		// TODO: If there are no errors, println("Success")
	}
}
