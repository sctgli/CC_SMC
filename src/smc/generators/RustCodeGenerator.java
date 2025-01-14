package smc.generators;

import smc.OptimizedStateMachine;
import smc.generators.nestedSwitchCaseGenerator.NSCNodeVisitor;
import smc.implementers.RustNestedSwitchCaseImplementer;

import java.io.IOException;
import java.nio.file.Files;
import java.util.Map;

public class RustCodeGenerator extends CodeGenerator {
  private RustNestedSwitchCaseImplementer implementer;

  public RustCodeGenerator(OptimizedStateMachine optimizedStateMachine,
                        String outputDirectory,
                        Map<String, String> flags) {
    super(optimizedStateMachine, outputDirectory, flags);
    implementer = new RustNestedSwitchCaseImplementer(flags);
  }

  protected NSCNodeVisitor getImplementer() {
    return implementer;
  }

  public void writeFiles() throws IOException {
    if (implementer.getErrors().size() > 0) {
      for (RustNestedSwitchCaseImplementer.Error error : implementer.getErrors())
        System.out.println("Implementation error: " + error.name());
    } else {
      String fileName = optimizedStateMachine.header.fsm.toLowerCase();
      Files.write(getOutputPath(fileName + ".rs"), (implementer.getFsmHeader() + "\n" + implementer.getFsmImplementation()).getBytes());
    }
  }
}
