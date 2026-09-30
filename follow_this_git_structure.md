# 🌿 DraftIt — Complete Git Guide (For Beginners)

> **This file is for two audiences:**
> 1. **You (Dhanu):** A complete, no-jargon guide to Git and how to use it for DraftIt.
> 2. **AI Agents:** Rules about which branch to work on (see the Agent Rules section at the bottom).

---

## 📖 Part 1: What Even Is Git?

Think of Git like a **save system in a video game**.

- Every time you **commit**, you create a save point.
- You can go back to any save point if something breaks.
- Your code is stored both on your computer (**local**) and on GitHub (**remote/cloud**).
- **Branches** are like parallel save files — you can experiment on one without ruining the other.

**GitHub** is just a website that stores your Git history in the cloud. It's your backup.

---

## 🔤 Part 2: Git Words Explained (The Dictionary)

### `commit` — "Save my work"
A **commit** is a snapshot of your code at a specific moment. It's like pressing "Save" in a game. Each commit has a message describing what you changed.

```bash
git commit -m "fixed the crash on settings screen"
```
This saves your changes **locally on your computer only**. It does NOT upload to GitHub yet.

---

### `add` — "Choose what to save"
Before you can commit (save), you need to tell Git **which files** to include in the save. This is called "staging."

```bash
git add .
```
The `.` means "add everything I changed." You can also add specific files:
```bash
git add app/src/main/java/com/flash/draft/SomeFile.kt
```

**Think of it like this:**
- `git add` = putting items into a box
- `git commit` = sealing the box and labeling it

---

### `push` — "Upload my saves to GitHub"
A **push** uploads your local commits (saves) to GitHub so they're backed up in the cloud.

```bash
git push
```

**Commit vs Push:**
- `commit` = save locally (only on your laptop)
- `push` = upload to GitHub (safe even if your laptop dies)

---

### `pull` — "Download the latest code from GitHub"
If you (or the AI agent) made changes from another device or tool, you need to download them first.

```bash
git pull
```

**Always pull before you start coding** if there's any chance someone else (or an AI) pushed changes.

---

### `branch` — "A parallel copy of your code"
A **branch** is like a parallel universe for your code. You can have multiple branches, each with different versions of your app.

```bash
git branch              # shows all branches (* = current one)
git branch --show-current   # shows only the current branch name
```

**Your branches:**
| Branch | What it is |
|---|---|
| `develop` | Your working copy — code here freely |
| `staging` | Your testing copy — test the full app here |
| `release` | Your Play Store copy — only perfect code goes here |

---

### `checkout` — "Switch to a different branch"
**Checkout** means "switch to" or "go to" a different branch. It changes all the files on your computer to match that branch.

```bash
git checkout develop     # switch to the develop branch
git checkout staging     # switch to the staging branch
git checkout release     # switch to the release branch
```

**Important:** Before switching branches, make sure you've committed (saved) your work. Otherwise your changes might get lost or cause conflicts.

```bash
# SAFE way to switch branches:
git add .
git commit -m "saving before switching"
git checkout staging
```

---

### `merge` — "Copy changes from one branch into another"
**Merge** takes the code from one branch and combines it into the branch you're currently on.

```bash
git checkout staging     # first, go to the branch you want to UPDATE
git merge develop        # then, bring in changes FROM develop
```

**Analogy:** Imagine `develop` is your rough draft and `staging` is your clean copy. Merge = copying the rough draft into the clean copy.

**The order matters:**
1. First `checkout` the branch you want to **receive** the changes
2. Then `merge` the branch you want to **take** changes from

---

### `stash` — "Hide my changes temporarily"
Sometimes you need to switch branches but you have unfinished work that you don't want to commit yet.

```bash
git stash                # hides your uncommitted changes
git checkout staging     # now you can safely switch
# ... do stuff on staging ...
git checkout develop     # come back to develop
git stash pop            # bring your hidden changes back
```

Think of `stash` like putting your work in a drawer temporarily.

---

### `status` — "What's going on right now?"
Shows you what files have changed, what's staged for commit, and which branch you're on.

```bash
git status
```

This is your **best friend**. Run it anytime you're confused.

---

### `log` — "Show me my save history"
Shows your commit history (all your past saves).

```bash
git log --oneline -10    # show last 10 commits, one per line
```

---

### `rebase` — "Advanced merge (you DON'T need this)"

> ⚠️ **You can safely ignore rebase.** It's an advanced way to merge that rewrites history. Merge is simpler and safer for you. If anyone (or any AI) suggests rebase, just use `merge` instead.

---

### `clone` — "Download a project for the first time"
You already have the project, so you don't need this. But for reference:
```bash
git clone https://github.com/Fordhamgr/draftit.git
```
This downloads the entire project and all its history from GitHub.

---

### `remote` — "The connection to GitHub"
Your project is connected to GitHub. The connection is called a "remote" and it's named `origin`.
```bash
git remote -v    # shows where your code uploads to
```

---

## 🎯 Part 3: Your DraftIt Workflow (Step by Step)

### The Big Picture

```
     YOU CODE HERE
          │
          ▼
┌─────────────────┐
│    develop       │  ← Write code, break things, experiment
│    (your desk)   │
└────────┬────────┘
         │  When a feature is done, MERGE DOWN ↓
         ▼
┌─────────────────┐
│    staging       │  ← Test the whole app, find bugs
│    (test lab)    │
└────────┬────────┘
         │  When everything is perfect, MERGE DOWN ↓
         ▼
┌─────────────────┐
│    release       │  ← Build the Play Store APK here
│    (the vault)   │
└─────────────────┘
```

---

### 🔧 Scenario 1: "I just want to write code and save it"

**Where you are:** `develop` branch (always code here)

**Steps:**
```bash
# Step 1: Make sure you're on develop
git checkout develop

# Step 2: Write your code in Android Studio... (just normal coding)

# Step 3: When you want to save, run these ONE BY ONE:
git add .
git commit -m "added new button to canvas"
git push
```

**What just happened:**
- `git add .` → selected all your changed files
- `git commit -m "..."` → saved them as a checkpoint on your laptop
- `git push` → uploaded that checkpoint to GitHub

---

### 🧪 Scenario 2: "My feature is done. I want to test it before releasing"

**Where you are:** `develop` branch
**Where you want the code to go:** `staging` branch

**Steps:**
```bash
# Step 1: Make sure all your work on develop is saved first
git add .
git commit -m "feature complete: dark mode"
git push

# Step 2: Switch to staging
git checkout staging

# Step 3: Pull the latest staging code (in case you forgot something last time)
git pull

# Step 4: Bring your develop code INTO staging
git merge develop

# Step 5: Upload the merged code to GitHub
git push

# Step 6: Go back to develop (so you don't accidentally code on staging)
git checkout develop
```

**Now you can test the app from the `staging` branch.** Open the project, build it, test everything.

---

### 🐛 Scenario 3: "I found a bug while testing on staging"

**DON'T fix it on staging.** Go back to develop, fix it there, then merge again.

**Steps:**
```bash
# Step 1: Go back to develop
git checkout develop

# Step 2: Fix the bug in Android Studio...

# Step 3: Save the fix
git add .
git commit -m "fixed crash when opening dark mode"
git push

# Step 4: Send the fix to staging again
git checkout staging
git merge develop
git push

# Step 5: Go back to develop
git checkout develop
```

**Now test the app again from staging.** Repeat until all bugs are fixed.

---

### 🚀 Scenario 4: "Everything works perfectly. Time to release to Play Store"

**Where you are:** `develop` branch
**Staging has been tested and works perfectly.**

**Steps:**
```bash
# Step 1: Switch to release
git checkout release

# Step 2: Pull the latest release code
git pull

# Step 3: Bring the tested staging code INTO release
git merge staging

# Step 4: Upload to GitHub
git push

# Step 5: Build your Play Store APK/AAB from this branch
# (In Android Studio: Build → Generate Signed Bundle / APK)

# Step 6: Go back to develop for future coding
git checkout develop
```

**Your release branch now has the exact code that's on the Play Store.**

---

### 😰 Scenario 5: "I'm lost and don't know what branch I'm on"

```bash
# Check your current branch
git branch --show-current

# See all branches
git branch

# See what files have changed
git status
```

---

### 😱 Scenario 6: "I accidentally started coding on staging or release"

**Don't panic.** Your changes are NOT saved yet (unless you committed).

```bash
# Step 1: Hide your changes temporarily
git stash

# Step 2: Go to develop
git checkout develop

# Step 3: Bring your changes back
git stash pop

# Now continue coding on develop normally
```

---

### 🤯 Scenario 7: "Git says MERGE CONFLICT"

This happens when the same line of code was changed in two different places and Git doesn't know which version to keep.

**What you'll see in the file:**
```
<<<<<<< HEAD
// This is the code from the branch you're ON
val color = Color.Red
=======
// This is the code from the branch you're MERGING
val color = Color.Blue
>>>>>>> develop
```

**How to fix it:**
1. Open the file in Android Studio
2. Decide which version you want (or combine them)
3. Delete the `<<<<<<<`, `=======`, and `>>>>>>>` marker lines
4. Save the file
5. Then run:
```bash
git add .
git commit -m "resolved merge conflict"
git push
```

---

### ⏪ Scenario 8: "I want to undo my last commit"

**If you HAVEN'T pushed yet:**
```bash
git reset --soft HEAD~1
```
This removes the commit but keeps your files unchanged. You can re-commit properly.

**If you HAVE pushed already:**
Don't try to undo it. Just make a new commit that fixes whatever was wrong.

---

## 📋 Part 4: Quick Reference Card

### 🔑 The Commands You'll Actually Use

| What you want to do | Command | What it means |
|---|---|---|
| Check which branch I'm on | `git branch --show-current` | "Where am I?" |
| Switch to develop | `git checkout develop` | "Go to my coding branch" |
| See what I changed | `git status` | "What files did I touch?" |
| Save my work locally | `git add .` then `git commit -m "message"` | "Stage and save" |
| Upload to GitHub | `git push` | "Back up to the cloud" |
| Download latest from GitHub | `git pull` | "Get the newest code" |
| Send develop → staging | `git checkout staging` then `git merge develop` then `git push` | "Send to testing" |
| Send staging → release | `git checkout release` then `git merge staging` then `git push` | "Send to Play Store" |
| Go back to coding | `git checkout develop` | "Go back to my desk" |
| Hide work temporarily | `git stash` | "Put in drawer" |
| Get hidden work back | `git stash pop` | "Take out of drawer" |

---

### 🚫 Things You Should NEVER Do

| Never do this | Why |
|---|---|
| Code directly on `release` | This is your Play Store build — keep it clean |
| Code directly on `staging` | Always fix bugs on `develop` and merge |
| Use `git push --force` | This can permanently delete history |
| Delete the `release` branch | You'll lose your Play Store baseline |
| Use `rebase` | It's advanced and can cause problems — use `merge` |

---

## 🤖 Part 5: Rules for AI Agents

> **⛔ NON-NEGOTIABLE:** All AI code changes MUST happen on the `develop` branch ONLY.

### Before writing ANY code, AI agents MUST:
1. Run `git branch --show-current` to verify the current branch.
2. If NOT on `develop`, run `git checkout develop` first.
3. **NEVER** commit, push, or modify code on `staging`, `release`, or `master`.

### If the user asks to make changes while on `staging` or `release`:
1. Switch to `develop` first.
2. Make the changes on `develop`.
3. Tell the user they can merge into staging/release when ready.

### After completing changes, the AI agent should:
1. `git add .` only the files that were modified.
2. `git commit -m "descriptive message"` with a clear description.
3. `git push` to upload to GitHub.
4. Confirm to the user that changes are saved on `develop`.

**Violating these rules risks breaking the production Play Store build.**
