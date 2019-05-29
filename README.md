# NoteSlurp - Paperless Evernote CLI

## Overview

NoteSlurp is a solution for importing various scanned and downloaded documents into Evernote efficiently, while automatically
 tagging based on naming conventions.

### The Problem

Premium Evernote provides an excellent platform for paperless storage of documents (especially considering the OCR, tagging, and search features),
but getting the documents into Evernote can be a pain. There are existing solutions, but none of them are ideal:

* Windows users can use the "Import Folder" feature of the Windows Evernote app, but users running Linux or Mac OS
don't have that luxury. Further, this simply dumps them into the Inbox notebook, without any tagging.
* On Windows or Mac OS, files can be dropped onto the desktop application notebooks, but this is not ideal
either, as it requires moving every file individually. Additionally, this does not work through the web-browser, and therefore
doesn't help Linux users.
* There are possible solutions when combining a service like "Dropbox" and "Zapier", but that requires a minimum of three
separate companies interacting with your (potentially sensitive) documents, and there are capacity limits with all of
these services, meaning it may cost additional money. It seems like overkill (though it is probably the most efficient solution for
Linux Evernote users)
* The Evernote mobile app has a scanning camera feature, but it can be challenging to use right and get high-quality attachments, and
this provides no benefit for downloaded PDFs (like statements from a utility company's website, for example)

### NoteSlurp to the Rescue

NoteSlurp is a portable application that only requires a JVM be installed (it should work anywhere that can do Java and HTTPS)
that can upload files to Evernote automatically (and when triggered by a CRON-like agent, on a regular basis).

To further optimize the import process, NoteSlurp can also automatically tag based on a keyword configuration that can
look for patterns in folder names and file paths to automatically select tag names.

## Installation

Generally on a Unix style system it's best to put the NoteSlurp.jar into your home directory or similar, and then
create an alias or shell for executing it. For example:

```bash
~ ❯❯❯ cat ~/bin/noteslurp
!#/bin/bash
java -jar /home/realjenius/noteslurp.jar $@
```

This allows simply typing `noteslurp` in your command line.

## Usage

Basic usage instructions can be found on the command-line:

```
~ ❯❯❯ noteslurp
Usage: noteslurp [OPTIONS] COMMAND [ARGS]...

Options:
  --debug            Enable debug logging
  --config-dir TEXT  The configuration directory (defaults to
                     <userhome>/.noteslurp)
  -h, --help         Show this message and exit

Commands:
  add-tags     Add auto-tagging configurations
  export-tags  Export the current tag definition
  file-notes   File notes, potentially adjusting tags as you go
  import-tags  Import a tags export file
  list-tags    List the current tag configurations
  remove-tags  Remove one to many tag configurations
  reset        Remove all configuration settings
  run          Slurp notes into Evernote
  set-env      Initialize Evernote authentication for an environment
  test-tags    Prints the tags that are computed off the given name for the
               current configuration
```

A first time run will typically involve this series of steps:

* `noteslurp set-env` to install a developer token (more on this below)
* Zero to many calls to `noteslurp add-tags` (alternatively `import-tags` can be used to bulk load via JSON)
* Executing an import via `noteslurp run`

## Configuration

### Developer Tokens

At this time, NoteSlurp only supports [Evernote developer tokens](http://dev.evernote.com/doc/articles/dev_tokens.php) for uploading. To use this tool currently, a support ticket must
be opened with the Evernote support staff to obtain developer token access for production. This usually takes a couple days.

Once the token is generated, it can be installed via this command:

```bash
noteslurp set-env --environment PRODUCTION --dev-token <devtoken-here>
```

Alternatively, the sandbox environment can be used and tried without any support requests to verify NoteSlurp setup (though it cannot be used for
real Evernote filing).

### Tags and Folder Paths

NoteSlurp can be enabled to look at both the relative folder path from the sync directory up to the file as well as the
file name itself when computing tag names.

For example, this call could be made to add tag mappings for your use:

```
noteslurp add-tags --folder \
  --keyword utility "Utility Bill" \
  --keyword gas "Gas Company" \
  --keyword 2018 "2018"
```

Then, given a file on a path like this: `<sync directory>/2018/utility/gas_bill.pdf` all three tags would be added.

(Note that "add-tags" is additive, so it can be called multiple times to keep adding tag mappings)

### Regex Tags

There is a special form of keyword tag based on regular expressions that can perform group capturing to auto-generate tags.

A common use-case of this is for month/year naming, as that is a popular tagging method in Evernote. For example, this tag configuration
would automatically match any combination of month and year and generate a corresponding tag:

```
noteslurp add-tags --regex-keyword ([01]\d)[-_](20\d\d) {1}-{0}
```

This says "for any pattern that matches ##-#### or ##_#### (with some additional number restrictions), create a tag
that matches ####-##".

So, for example: `11_2019` in either the folder name or the file name would result in the tag '2019-11' on the Note.

### Example Tag Configuration

This is an example tag configuration that can be imported as a starting point. This includes these tag generators:

* Several named tags including `taxes` -> `Taxes`
* Month/Year regex matching: `02_2018` -> `2018-02`, `2019-03` -> `2019-03` (etc)
* Folders are enabled so this is possible as well: `/2018-02/taxes/my-file.pdf`

```javascript
{
  "folderTags" : true,
  "keywords" : [ {
    "mapping" : "([01]\\d)[-_](20\\d\\d)",
    "target" : "{1}-{0}",
    "type" : "Regex"
  }, {
    "mapping" : "(20\\d\\d)[-_]([01]\\d)",
    "target" : "{0}-{1}",
    "type" : "Regex"
  }, {
    "mapping" : "taxes",
    "target" : "Taxes",
    "type" : "Text"
  }, {
    "mapping" : "retirement",
    "target" : "Retirement",
    "type" : "Text"
  }, {
    "mapping" : "receipt",
    "target" : "Receipts",
    "type" : "Text"
  }, {
    "mapping" : "pets",
    "target" : "Pet Care",
    "type" : "Text"
  }, {
    "mapping" : "pediatrics",
    "target" : "Pediatrics",
    "type" : "Text"
  }, {
    "mapping" : "vacation",
    "target" : "Vacation",
    "type" : "Text"
  }, {
    "mapping" : "travel",
    "target" : "Travel",
    "type" : "Text"
  }, {
    "mapping" : "medical",
    "target" : "Medical",
    "type" : "Text"
  }, {
    "mapping" : "legal",
    "target" : "Legal",
    "type" : "Text"
  }, {
    "mapping" : "insurance",
    "target" : "Insurance",
    "type" : "Text"
  }, {
    "mapping" : "financial",
    "target" : "Financial",
    "type" : "Text"
  }, {
    "mapping" : "charity",
    "target" : "Charity",
    "type" : "Text"
  }, {
    "mapping" : "security",
    "target" : "Security",
    "type" : "Text"
  }, {
    "mapping" : "hoa",
    "target" : "HOA",
    "type" : "Text"
  }, {
    "mapping" : "home",
    "target" : "Home",
    "type" : "Text"
  }],
  "version" : 1
}
```

### Note Format

The resulting notes created in Evernote currently have a title to match the document, and have an empty body with only a
clickable link to the document itself. This is not currently customizable.

### Automating

Generally, once you have set up Noteslurp, a CRONtab entry is the best way to use it automatically. For example, from
my local environment I use this CRONtab entry to execute it every 5 minutes and sync notes from a directory in
my home folder:

```bash
*/5 * * * * java -jar /home/realjenius/noteslurp.jar \
  --config-dir /home/realjenius/.noteslurp \
  run \
  --from /home/realjenius/Documents/sync
```

This will copy documents from the `~/Documents/sync` dir every 5 minutes into Evernote, using the configuration
previously specified and all of the other previous tag configuration.

### Filing Notes (Experimental)

The other major component of NoteSlurp is the ability to easily file notes. This is still in relatively early
development, but is used by me regularly. To use this interactive command line tool, start with the `file-notes` command:

```
noteslurp file-notes --dest Cabinet
```

If you don't specify a "source" notebook, NoteSlurp assumes your "Default" notebook (usually the inbox or similar).

Once started the logs should look like this:

```bash
22:09:21.800 [main] INFO  r.e.n.evernote.EvernoteNoteAdjuster - Filing 32 notes from:
	'@Inbox' (31d7c3b3-0f2c-460f-90d1-bd920117dafd) into:
	'Cabinet' (a88e0cb4-4fe2-4323-ba04-00d68f957abc)
Note: 'bestbuy_credit_statement_05_2019.PDF' (created at: 2019-05-25 12:15:11) with Tags: '[2019-05] - Action: (M/S/D/C/T/Q):m
Note: 'anotherdocument-05-2019.pdf' (created at: 2019-05-25 12:15:10) with Tags: '[Kindergarten , 2019-05, 3rd Grade] - Action: (M/S/D/C/T/Q):m
Note: 'yetanother-05-2019.PDF' (created at: 2019-05-25 12:15:09) with Tags: '[Insurance, 2019-05] - Action: (M/S/D/C/T/Q):m
Note: 'something-house-related_05_2019.PDF' (created at: 2019-05-25 12:15:08) with Tags: '[Home, 2019-05] - Action: (M/S/D/C/T/Q):m
```

The actions are:

Action | Description
--- | ---
M | Move to the destination, aka "looks good!"
S | Skip this document - need to review in Evernote
D | Delete this document - it was a mistake upload
Q | Quit note filing, I'm bored or tired
T | Change the title of the note - The new title will be rescanned for tag keywords!
C | Manually change the tags of the document

Of the above, the only one with esoteric syntax it 'C'. When changing tags at the prompt you can add and remove tags using
the pre-built keywords, and you use `+|-` prefixes to add or remove. For example, to add the "Insurance" tag and the "2019-04" tag
but remove the "2019-03" tag it might look like this:

```bash
Tag Changes:+insur +2019-04 -2019-03
```

(Assuming insur is a keyword tag for Insurance)