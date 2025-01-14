/*
 * SonarQube Java
 * Copyright (C) 2012-2022 SonarSource SA
 * mailto:info AT sonarsource DOT com
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 3 of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301, USA.
 */
package org.sonar.java.checks;

import org.apache.commons.lang3.StringUtils;
import org.sonar.check.Rule;
import org.sonar.check.RuleProperty;
import org.sonar.java.checks.methods.AbstractMethodDetection;
import org.sonar.plugins.java.api.semantic.MethodMatchers;
import org.sonar.plugins.java.api.tree.NewClassTree;

@Rule(key = "S4011")
public class DisallowedConstructorCheck extends AbstractMethodDetection {

  @RuleProperty(key = "className", description = "Name of the class whose constructor is forbidden. This parameter is mandatory, if absent the rule is disabled.")
  private String className = "";

  @RuleProperty(key = "argumentTypes", description = "Comma-delimited list of argument types, E.G. java.lang.String, int[], int")
  private String argumentTypes = "";

  @RuleProperty(key = "allOverloads", description = "Set to true to flag all overloads regardless of parameter type", defaultValue = "false")
  private boolean allOverloads = false;

  @Override
  protected MethodMatchers getMethodInvocationMatchers() {
    if (StringUtils.isEmpty(className)) {
      return MethodMatchers.none();
    }
    MethodMatchers.ParametersBuilder invocationMatcher = MethodMatchers.create().ofTypes(className).constructor();
    if (allOverloads) {
      return invocationMatcher.withAnyParameters().build();
    } else {
      String[] args = StringUtils.split(argumentTypes, ",");
      if (args.length == 0) {
        return invocationMatcher.addWithoutParametersMatcher().build();
      } else {
        String[] trimmedArgs = new String[args.length];
        for (int i = 0; i < trimmedArgs.length; i++) {
          trimmedArgs[i] = StringUtils.trim(args[i]);
        }
        return invocationMatcher.addParametersMatcher(trimmedArgs).build();
      }
    }
  }

  @Override
  protected void onConstructorFound(NewClassTree newClassTree) {
    reportIssue(newClassTree.identifier(), "Remove this forbidden initialization");
  }

  public void setClassName(String className) {
    this.className = className;
  }

  public void setArgumentTypes(String argumentTypes) {
    this.argumentTypes = argumentTypes;
  }

  public void setAllOverloads(boolean allOverloads) {
    this.allOverloads = allOverloads;
  }
}
