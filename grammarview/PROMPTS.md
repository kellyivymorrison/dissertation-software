# User Prompts Log

## Day 1: 2026-03-10
- I want to create a new Java 20 command line app called grammarview that will read a YACC file and produce a PDF view of the grammar with custom symbols that I will define later. The app should have a -legend command line parameter for adding a legend page, a -help parameter for showing help, and a -v parameter to enable verbose logging. The command line also needs a mandatory argument that is the name of a YACC file to be processed.

## Day 2: 2026-03-11
- I have a saved Gemini chat called "day1". Could you use the information in that to update the HISTORY.md file with what we did yesterday?
- I want a .gitignore file for this project
- I want the PDF file to be created in landscape mode
- let's move that test.y file to an "examples" folder
- Create a README.md file for the project
- I want to use the US letter size page in the PDF, not A4
- If a YACC grammar rule has multiple right hand sides, put each on a separate line. For example, if a rules is X -> Y | Z, then X --> Y should be on one line and Z on the next.
- draw a rectangular box around each of the left hand side portions of a rule. The box should have a drop shadow.
- Draw an ellipse around each right hand side. Then connect them to the left hand side using a line.
- Instead of an oval, let's use a rounded rectangle with a light yellow background
- The text should be centered within the rectangles and rounded rectangles. Also, the yellow should be a more vivid yellow.
- create a file called PROMPTS.md in the project root, and add all of my prompts for each day into it. Also add all future prompts I enter into the file.
- The lines connecting the rectangles should only go up and down. No slanted lines.
- I would like each item on the right hand side of a rule to be in its own rounded rectangle.
- Terminals should be drawn with a rounded rectangle, but with a light blue background.
- I've updated test.y. Please create the PDF file for it, with a legend
- Instead of drawing a box with "epsilon" in it when there isn't a right hand side, just draw nothing in its place
- Add documentation to the code
- The rectangle for the left hand side for the starting (top level) rule in the grammar should be drawn so that the left hand side of the rectangle actually looks like a "less than" sign (<).
- The other left hand side symbols need a black border. It was there before, but disappeared when you made the last change.
- If any of the rhs alternative items is the same as the left hand side (e.g., xxx -> xxx item | item), then the symbol on the left hand size should be drawn with two offset rectangles behind it, each positioned a little to the right and a little above the rectangle in front of it. The idea is to denote that it can produce multiple copies of itself.
- run the program on all input files in the examples directory.
- I've updated test2.y. Please regenerate the PDF.
- The items on the left hand side of a rule (the rule name) are considered "recursive" if the rule name appears on both the left hand side and at least one of its right hand sides. If a rule name is determined to be recursive, then it should be drawn as a recursive symbol everywhere it is used in the grammar, both left and right hand sides. Use the same drawing style we used earlier for a recursive symbol.
- Recursive rule names should have the same yellow background color everywhere, whether on the left hand side or right hand side of a rule.
- If a rule has an empty right hand side alternative, it is considered "nullable". Nullable symbols should always be drawn with a light gray background. This light gray background takes precedence over the yellow background if the rule is also recursive.
- All text should be drawn in bold. Also, the text still doesn't seem to be quite centered in the symbols.
- All of the rule names on the left hand side should have a yellow background (unless they are nullable: then keep the light gray background).
- Please document the program again, as you removed the documentation you added previously.
- I would like to add the following header to all Java source files:
# Copyright (c) 2026 Kelly Morrison
#
# This program is free software: you can redistribute it and/or modify
# it under the terms of the GNU General Public License version 3.
- # Copyright (c) 2026 Kelly Morrison
#
# This program is free software: you can redistribute it and/or modify
# it under the terms of the GNU General Public License version 3.
- Update the README.md to use GPL-3.0
- Also add the following text to the README.md file:
This software is released under GPL-3.0 to ensure that improvements
remain open and available to the research community.
- The LICENSE.txt file should not have an extension. It should just be LICENSE
- If the grammar or lexer you downloaded for YACC/Bison need to be attributed, please add the attributions to the README.md file
- I would like to add a Dockerfile and the ability to run the program on the command line from Docker. It should use a lightweight Docker image, and it should use openjdk for Java 20 (or higher). I would also like instructions in the README.md file on how to use it this way.
- When I try to run the program using your example command line, I get this error: Error: Unable to access jarfile grammarview.jar
- Is dependency-reduced-pom.xml still needed? If not, I'd like to delete it.

## Day 6: 2026-03-15
- I want you to save my prompts in PROMPTS.md
- I would like to replace the hard coded "magic numbers" with well-named constants.
- Let's move the routines that draw PDF graphics into a PdfSymbolRenderer class.
- The program should catch all exceptions and return an appropriate Linux return code for the various types of error as well as printing a relevant message to stderr. For example, when an invalid argument is presented, when a YACC grammar cannot be parsed, when an error happens creating the PDF file.
- Please document the return codes in README.md
- I would like to be able to specify the font size from the command line, but I'm not sure how that should look. What is the standard or common UNIX way to specify that?
- Yes. Let's allow font sizes from 6 to 32, and let's keep the existing font size as the default.
- Why is there not a constant for the return code of 2? Please fix that, and keep it consistent with the other return codes.
- I would also like to add a command line option to draw the PDF in portrait mode instead of landscape.
- I would like to be able to specify a page size from the command line. Keep the LETTER size as the default, but add support for the page sizes supported by Apache PDFBox.
- I would like to add some unit tests. For example, for input file test2.y, I want to verify that march is the start symbol; that there are 8 rules; that "embellishment" is nullable; and that "steps" is recursive.
- Add similar unit tests for test.y. Also, please document the unit tests with GIVEN.. WHEN... THEN... comments.
- If a rhs is empty, or if all of the items on a rhs alternative are nullable, then the rule is nullable.

