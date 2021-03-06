/*
 * Copyright 2012 Adam Murdoch
 *
 *    Licensed under the Apache License, Version 2.0 (the "License");
 *    you may not use this file except in compliance with the License.
 *    You may obtain a copy of the License at
 *
 *        http://www.apache.org/licenses/LICENSE-2.0
 *
 *    Unless required by applicable law or agreed to in writing, software
 *    distributed under the License is distributed on an "AS IS" BASIS,
 *    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *    See the License for the specific language governing permissions and
 *    limitations under the License.
 */

package net.rubygrapefruit.platform

import spock.lang.Specification
import org.junit.Rule
import org.junit.rules.TemporaryFolder
import spock.lang.IgnoreIf
import net.rubygrapefruit.platform.internal.Platform

@IgnoreIf({Platform.current().windows})
class PosixFilesTest extends Specification {
    @Rule TemporaryFolder tmpDir
    final PosixFiles file = Native.get(PosixFiles.class)

    def "caches file instance"() {
        expect:
        Native.get(PosixFiles.class) == file
    }

    def "can get details of a file"() {
        def testFile = tmpDir.newFile(fileName)

        when:
        def stat = file.stat(testFile)

        then:
        stat.type == PosixFile.Type.File
        stat.mode != 0

        where:
        fileName << ["test.txt", "test\u03b1\u2295.txt"]
    }

    def "can get details of a directory"() {
        def testFile = tmpDir.newFolder(fileName)

        when:
        def stat = file.stat(testFile)

        then:
        stat.type == PosixFile.Type.Directory
        stat.mode != 0

        where:
        fileName << ["test-dir", "test\u03b1\u2295-dir"]
    }

    def "can get details of a missing file"() {
        def testFile = new File(tmpDir.root, fileName)

        when:
        def stat = file.stat(testFile)

        then:
        stat.type == PosixFile.Type.Missing
        stat.mode == 0

        where:
        fileName << ["test-dir", "test\u03b1\u2295-dir"]
    }

    def "can set mode on a file"() {
        def testFile = tmpDir.newFile(fileName)

        when:
        file.setMode(testFile, 0740)

        then:
        file.getMode(testFile) == 0740
        file.stat(testFile).mode == 0740

        where:
        fileName << ["test.txt", "test\u03b1\u2295.txt"]
    }

    def "can set mode on a directory"() {
        def testFile = tmpDir.newFolder(fileName)

        when:
        file.setMode(testFile, 0740)

        then:
        file.getMode(testFile) == 0740
        file.stat(testFile).mode == 0740

        where:
        fileName << ["test-dir", "test\u03b1\u2295-dir"]
    }

    def "cannot set mode on file that does not exist"() {
        def testFile = new File(tmpDir.root, "unknown")

        when:
        file.setMode(testFile, 0660)

        then:
        NativeException e = thrown()
        e.message == "Could not set UNIX mode on $testFile: could not chmod file (ENOENT errno 2)"
    }

    def "cannot get mode on file that does not exist"() {
        def testFile = new File(tmpDir.root, "unknown")

        when:
        file.getMode(testFile)

        then:
        NativeException e = thrown()
        e.message == "Could not get UNIX mode on $testFile: file does not exist."
    }

    def "can create symbolic link"() {
        def testFile = new File(tmpDir.root, "test.txt")
        testFile.text = "hi"
        def symlinkFile = new File(tmpDir.root, "symlink")

        when:
        file.symlink(symlinkFile, testFile.name)

        then:
        symlinkFile.file
        symlinkFile.text == "hi"
        symlinkFile.canonicalFile == testFile.canonicalFile
    }

    def "can read symbolic link"() {
        def symlinkFile = new File(tmpDir.root, "symlink")

        when:
        file.symlink(symlinkFile, "target")

        then:
        file.readLink(symlinkFile) == "target"
    }

    def "cannot read a symlink that does not exist"() {
        def symlinkFile = new File(tmpDir.root, "symlink")

        when:
        file.readLink(symlinkFile)

        then:
        NativeException e = thrown()
        e.message == "Could not read symlink $symlinkFile: could not lstat file (ENOENT errno 2)"
    }

    def "cannot read a symlink that is not a symlink"() {
        def symlinkFile = tmpDir.newFile("not-a-symlink.txt")

        when:
        file.readLink(symlinkFile)

        then:
        NativeException e = thrown()
        e.message == "Could not read symlink $symlinkFile: could not readlink (errno 22)"
    }

    def "can create and read symlink with unicode in its name"() {
        def testFile = new File(tmpDir.root, "target\u03b2\u2295")
        testFile.text = 'hi'
        def symlinkFile = new File(tmpDir.root, "symlink\u03b2\u2296")

        when:
        file.symlink(symlinkFile, testFile.name)

        then:
        file.readLink(symlinkFile) == testFile.name
        symlinkFile.file
        symlinkFile.canonicalFile == testFile.canonicalFile
    }

    def "can get details of a symlink"() {
        def testFile = new File(tmpDir.newFolder("parent"), fileName)

        given:
        file.symlink(testFile, "target")

        when:
        def stat = file.stat(testFile)

        then:
        stat.type == PosixFile.Type.Symlink
        stat.mode != 0

        where:
        fileName << ["test.txt", "test\u03b1\u2295.txt"]
    }
}
