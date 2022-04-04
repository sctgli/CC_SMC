package smc.implementers;

import smc.Utilities;
import smc.generators.nestedSwitchCaseGenerator.NSCNodeVisitor;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static smc.generators.nestedSwitchCaseGenerator.NSCNode.*;

public class RustNestedSwitchCaseImplementer implements NSCNodeVisitor {
  private String fsmName;
  private String actionsName;
  private String fsmHeader = "";
  private String fsmImplementation = "";
  private List<Error> errors = new ArrayList<>();
  private Map<String, String> flags;

  public RustNestedSwitchCaseImplementer(Map<String, String> flags) {
    this.flags = flags;
  }

  public void visit(SwitchCaseNode switchCaseNode) {
    fsmImplementation += String.format("match %s {\n", switchCaseNode.variableName);
    switchCaseNode.generateCases(this);
    fsmImplementation += "}\n";
  }

  public void visit(CaseNode caseNode) {
    fsmImplementation += String.format("%s => {\n", caseNode.caseName);
    caseNode.caseActionNode.accept(this);
    fsmImplementation += "}\n\n";
  }

  public void visit(FunctionCallNode functionCallNode) {
    fsmImplementation += String.format("self.%s(", functionCallNode.functionName);
    if (functionCallNode.argument != null) {
      // fsmImplementation += ", ";
      functionCallNode.argument.accept(this);
    }
    fsmImplementation += ");\n";
  }

  public void visit(EnumNode enumNode) {
    fsmImplementation += String.format("#[derive(Copy, Clone)]\nenum %s {%s}\n", enumNode.name, Utilities.commaList(enumNode.enumerators));
  }

  public void visit(StatePropertyNode statePropertyNode) {
    fsmImplementation +=
            "impl<'a> " + fsmName + "<'a> {\n" +
      String.format("fn new(actions:&'a mut dyn %s) -> Self {\n", actionsName) +
        String.format("\t%s {\n", fsmName) +
        String.format("\tactions,\n") +
        String.format("\tstate: State::%s,\n}\n", statePropertyNode.initialState) + "}\n\n";

    fsmImplementation += String.format("" +
      "fn setState(&mut self, it:State) {\n" +
      "\tself.state = it;\n" +
      "}\n\n", fsmName);
  }

  public void visit(EventDelegatorsNode eventDelegatorsNode) {
    for (String event : eventDelegatorsNode.events) {
//      fsmHeader += String.format("void %s_%s(struct %s*);\n", fsmName, event, fsmName);

      fsmImplementation += String.format("" +
        "pub fn %s(&mut self) {\n" +
        "\tself.processEvent(self.state, %s, \"%s\");\n" +
        "}\n", event, event, event);
    }
  }

  public void visit(FSMClassNode fsmClassNode) {
    if (fsmClassNode.actionsName == null) {
      errors.add(Error.NO_ACTION);
      return;
    }
    actionsName = fsmClassNode.actionsName;
    fsmName = fsmClassNode.className;

//    fsmImplementation += "#include <stdlib.h>\n";
//    fsmImplementation += String.format("#include \"%s.h\"\n", actionsName);
//    fsmImplementation += String.format("#include \"%s.h\"\n\n", fsmName);
    fsmClassNode.eventEnum.accept(this);
    fsmClassNode.stateEnum.accept(this);

    fsmImplementation += String.format("\n" +
      "struct %s<'a> {\n" +
      "\tstate:State,\n" +
      "\tactions:&'a mut dyn %s,\n" +
      "}\n\n", fsmName, actionsName);

    fsmClassNode.stateProperty.accept(this);

    for (String action : fsmClassNode.actions) {
      fsmImplementation += String.format("" +
        "fn %s(&mut self) {\n" +
        "\tself.actions.%s();\n" +
        "}\n\n", action, action);
    }
    fsmClassNode.handleEvent.accept(this);

//    String includeGuard = fsmName.toUpperCase();
//    fsmHeader += String.format("#ifndef %s_H\n#define %s_H\n\n", includeGuard, includeGuard);
//    fsmHeader += String.format("struct %s;\n", actionsName);
//    fsmHeader += String.format("struct %s;\n", fsmName);
//    fsmHeader += String.format("struct %s *make_%s(struct %s*);\n", fsmName, fsmName, actionsName);
    fsmClassNode.delegators.accept(this);
//    fsmHeader += "#endif\n";
    
    fsmHeader += "use State::{*};";
    fsmHeader += "use Event::{*};";
  }

  public void visit(HandleEventNode handleEventNode) {
    fsmImplementation += String.format("" +
        "fn processEvent(&mut self, state:State, event:Event, event_name:&str) {\n",
      fsmName);
    handleEventNode.switchCase.accept(this);
    fsmImplementation += "}\n\n";
  }

  public void visit(EnumeratorNode enumeratorNode) {
    fsmImplementation += enumeratorNode.enumerator;
  }

  public void visit(DefaultCaseNode defaultCaseNode) {
    fsmImplementation += String.format("" +
      "_ => {\n" +
      "self.actions.unexpected_transition(\"%s\", event_name);\n" +
      "}\n", defaultCaseNode.state);
  }

  public String getFsmHeader() {
    return fsmHeader;
  }

  public String getFsmImplementation() {
    return fsmImplementation + "}";
  }

  public List<Error> getErrors() {
    return errors;
  }

  public enum Error {NO_ACTION}
}
