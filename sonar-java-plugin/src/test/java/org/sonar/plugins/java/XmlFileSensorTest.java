/*
 * SonarQube Java
 * Copyright (C) 2012-2021 SonarSource SA
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
package org.sonar.plugins.java;

import java.nio.charset.StandardCharsets;
import org.junit.Rule;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.junit.jupiter.migrationsupport.rules.EnableRuleMigrationSupport;
import org.junit.rules.TemporaryFolder;
import org.mockito.Mockito;
import org.sonar.api.batch.fs.internal.DefaultInputFile;
import org.sonar.api.batch.fs.internal.TestInputFileBuilder;
import org.sonar.api.batch.rule.ActiveRules;
import org.sonar.api.batch.rule.CheckFactory;
import org.sonar.api.batch.rule.internal.ActiveRulesBuilder;
import org.sonar.api.batch.rule.internal.NewActiveRule;
import org.sonar.api.batch.sensor.internal.DefaultSensorDescriptor;
import org.sonar.api.batch.sensor.internal.SensorContextTester;
import org.sonar.api.config.internal.MapSettings;
import org.sonar.api.rule.RuleKey;
import org.sonar.api.utils.log.LogTesterJUnit5;
import org.sonar.api.utils.log.LoggerLevel;
import org.sonarsource.analyzer.commons.xml.XmlFile;
import org.sonarsource.analyzer.commons.xml.checks.SonarXmlCheck;

import static org.assertj.core.api.Assertions.assertThat;

@EnableRuleMigrationSupport
class XmlFileSensorTest {

  @Rule
  public TemporaryFolder temporaryFolder = new TemporaryFolder();

  @RegisterExtension
  public LogTesterJUnit5 logTester = new LogTesterJUnit5();

  private static final RuleKey XML_RULE_KEY = RuleKey.of("java", "S3281");
  private SensorContextTester context;

  @BeforeEach
  void setUp() throws Exception {
    context = SensorContextTester.create(temporaryFolder.newFolder());
  }

  private ActiveRules activeRules() {
    ActiveRulesBuilder activeRulesBuilder = new ActiveRulesBuilder();
    activeRulesBuilder.addRule(new NewActiveRule.Builder().setRuleKey(XML_RULE_KEY).build());
    return activeRulesBuilder.build();
  }


  @Test
  void testDoNothingIfParsingError() throws Exception {
    CheckFactory checkFactory = new CheckFactory(activeRules());
    XmlFileSensor sensor = new XmlFileSensor(checkFactory);

    DefaultInputFile notXml = TestInputFileBuilder.create("moduleKey", "test.xml")
      .setCharset(StandardCharsets.UTF_8)
      .setContents("<ejb-jar")
      .build();
    context.fileSystem().add(notXml);

    sensor.execute(context);

    assertThat(context.allIssues()).isEmpty();
    assertThat(logTester.logs(LoggerLevel.DEBUG)).hasSize(1);
    assertThat(logTester.logs(LoggerLevel.DEBUG).get(0)).isEqualTo("Skipped 'test.xml' due to parsing error");
  }

  @Test
  void testDoNotFailAnalysisIfUnexpectedIssue() throws Exception {
    CheckFactory checkFactory = new CheckFactory(activeRules());
    XmlFileSensor sensor = new XmlFileSensor(checkFactory);

    DefaultInputFile inputFile = TestInputFileBuilder.create("moduleKey", "test.xml")
      .setCharset(StandardCharsets.UTF_8)
      .setContents(
        "<a>\n"
          + "  <b />\n"
          + "</a>\n")
      .build();
    DefaultInputFile mocked = Mockito.spy(inputFile);
    Mockito.when(mocked.contents()).thenThrow(new IllegalStateException("This should have been caught."));
    context.fileSystem().add(mocked);
    sensor.execute(context);

    assertThat(context.allIssues()).isEmpty();
    assertThat(logTester.logs(LoggerLevel.DEBUG)).isEmpty();
    assertThat(logTester.logs(LoggerLevel.WARN)).hasSize(1);
    assertThat(logTester.logs(LoggerLevel.WARN).get(0)).startsWith("Unable to analyse file 'test.xml'.");
  }

  @Test
  void testDescriptor() throws Exception {
    XmlFileSensor sensor = new XmlFileSensor(new CheckFactory(new ActiveRulesBuilder().build()));
    DefaultSensorDescriptor descriptor = new DefaultSensorDescriptor();
    sensor.describe(descriptor);

    assertThat(descriptor.name()).isEqualTo("JavaXmlSensor");
    // todo: do we want to run this sensor only for projects containing JVM languages
    assertThat(descriptor.languages()).isEmpty();
    assertThat(descriptor.isGlobal()).isFalse();
    assertThat(descriptor.configurationPredicate().test(new MapSettings().asConfig())).isFalse();

    // FIXME was checking activation as soon as there is an XML rule. TO BE DROPPED, like the rest of the class
    // sensor = new XmlFileSensor(new CheckFactory(activeRules()));
    // descriptor = new DefaultSensorDescriptor();
    // sensor.describe(descriptor);
    // assertThat(descriptor.languages()).isEmpty();
    // assertThat(descriptor.isGlobal()).isFalse();
    // assertThat(descriptor.configurationPredicate().test(new MapSettings().asConfig())).isTrue();

  }

  private static class TestCheck extends SonarXmlCheck {

    @Override
    public void scanFile(XmlFile file) {
      throw new IllegalStateException("Something wrong happened");
    }
  }

}
