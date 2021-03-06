/*
 * Copyright (c) 2011, Cloudera, Inc. All Rights Reserved.
 *
 * Cloudera, Inc. licenses this file to you under the Apache License,
 * Version 2.0 (the "License"). You may not use this file except in
 * compliance with the License. You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * This software is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR
 * CONDITIONS OF ANY KIND, either express or implied. See the License for
 * the specific language governing permissions and limitations under the
 * License.
 */

package com.cloudera.itest.shell

import org.apache.commons.logging.Log
import org.apache.commons.logging.LogFactory

class Shell {
  static private Log LOG = LogFactory.getLog(Shell.class)

  static private final String DEFAULT_SHELL = "/bin/bash -s";

  String shell;
  String user;

  String script;
  List<String> out;
  List<String> err;
  int ret;

  Shell(String sh) {
    shell = sh;
  }

  Shell(String sh, String u) {
    shell = sh;
    user = u;
  }

  Shell() {
    shell = DEFAULT_SHELL;
  }

  void setUser(String u) {
    user = u
  }

  /**
   * Execute shell script consisting of as many Strings as we have arguments,
   * possibly under an explicit username (requires sudoers privileges).
   * NOTE: individual strings are concatenated into a single script as though
   * they were delimited with new line character. All quoting rules are exactly
   * what one would expect in standalone shell script.
   *
   * After executing the script its return code can be accessed as getRet(),
   * stdout as getOut() and stderr as getErr(). The script itself can be accessed
   * as getScript()
   * WARNING: it isn't thread safe
   * @param args shell script split into multiple Strings
   * @return Shell object for chaining
   */
  Shell exec(Object... args) {
    def proc = user ? "sudo -u $user PATH=${System.getenv('PATH')} $shell".execute() :
                                    "$shell".execute()
    script = args.join("\n")
    if (LOG.isTraceEnabled()) {
        LOG.trace("${shell} << __EOT__\n${script}\n__EOT__");
    }

    Thread.start {
      def writer = new PrintWriter(new BufferedOutputStream(proc.out))
      writer.println(script)
      writer.close()
    }
    ByteArrayOutputStream baosErr = new ByteArrayOutputStream(4096);
    proc.consumeProcessErrorStream(baosErr);
    out = proc.in.readLines()

    // Possibly a bug in String.split as it generates a 1-element array on an
    // empty String
    if (baosErr.size() != 0) {
      err = baosErr.toString().split('\n');
    }
    else {
      err = new ArrayList<String>();
    }

    proc.waitFor()
    ret = proc.exitValue()

    if (LOG.isTraceEnabled()) {
        if (ret != 0) {
           LOG.trace("return: $ret");
        }
        if (out.size() != 0) {
           LOG.trace("\n<stdout>\n${out.join('\n')}\n</stdout>");
        }
        if (err.size() != 0) {
           LOG.trace("\n<stderr>\n${err.join('\n')}\n</stderr>");
        }
    }

    return this
  }
}
