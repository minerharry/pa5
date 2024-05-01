# Syntactic Analysis
Syntactic analysis was done using recursive analysis. If the compiler flag IS_MINI is unset, this can parse nearly every real java program correctly. The biggest missing elements are anonymous class declarations, inline block statements, and lambda expressions.

# AST Generation
We assume general familiarity with ASTs. See ASTChanges.txt for data beyond syntax that is stored in ASTs. Again, if IS_MINI is unset, this program will generate a correct AST for nearly every program.

# Contextual Analysis
Contextual analysis is performed in two traversals, scoped identification and type checking. Scoped identification only identifies naked identifiers and identifierTypes; qualifier references and qualified types are handled in type checking. Scoped Identification also injects the String and System classes into each package. Type Checking uses the associated declarations from scoped identification to handle qualified references to anonymous objects, e.g. objects returned from methods, base on their type.

# Code Generation
Few optimizations were done. Static fields (class bodies) are initialized onto the stack as the first instructions of the program, and a call() is made to the main method. Upon return, the program exits. Then, each method is laid out in sequence.

# Greedy Decisions / Gotchas
All pushes and pops are 8 bytes, as are loads and stores, so instance bodies and stack sizes are inefficient. Additionally, a malloc is performed per object instantiation, meaning large quantities of heap space are wasted. Arithmetic errors are unchecked! Manual error handling works correctly, though.