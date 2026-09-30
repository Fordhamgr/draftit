# 🌿 DraftIt — Git Branching Rules & Guide

> **This file is read by AI agents.** It tells them which branch to work on.
> **This file is also for YOU (Dhanu).** It teaches you how to move code between branches.

---

## ⛔ CRITICAL RULE FOR AI AGENTS

**ALL code changes MUST happen on the `develop` branch. ONLY.**

Before writing ANY code, the AI agent MUST:
1. Run `git branch --show-current` to check the current branch.
2. If the current branch is NOT `develop`, run `git checkout develop` before doing anything.
3. **NEVER** commit, push, or modify code on `staging`, `release`, or `master`.

If the user asks to make changes while on `staging` or `release`, the agent must:
- Switch to `develop` first
- Make the changes there
- Inform the user they can merge into staging/release when ready

**Violating this rule risks breaking the production Play Store build.**

---

## 📋 Branch Map

```
develop        ← 🔧 Daily coding happens HERE (AI works here)
   ↓ merge
staging        ← 🧪 Testing before release (you test the full app here)
   ↓ merge
release        ← 🚀 Final Play Store builds ONLY (sacred, never broken)
```

| Branch | What it's for | Who changes it |
|---|---|---|
| `develop` | Writing new code, fixing bugs, experimenting | You + AI agent |
| `staging` | Testing a feature before it goes to Play Store | You (by merging from develop) |
| `release` | The exact code that's on the Play Store | You (by merging from staging) |
| `master` | Old default branch — **don't use anymore** | Nobody |

---

## 🎓 Git Guide for Beginners (Step-by-Step)

### 📌 Before you start coding — make sure you're on `develop`

Open the **Terminal** tab in Android Studio (bottom of the screen) and type:

```bash
git checkout develop
```

You should see: `Switched to branch 'develop'`

---

### 💾 How to SAVE your work (Commit)

After you've made changes and they're working:

```bash
git add .
git commit -m "describe what you changed"
git push
```

**Example:**
```bash
git add .
git commit -m "added dark mode to settings screen"
git push
```

That's it! Your code is saved on GitHub on the `develop` branch.

---

### 🧪 How to send code to STAGING (for testing)

When a feature is done on `develop` and you want to test the full app:

```bash
git checkout staging
git merge develop
git push
git checkout develop
```

**What this does:**
1. Switches to the `staging` branch
2. Copies all your new code from `develop` into `staging`
3. Pushes it to GitHub
4. Switches you back to `develop` so you can keep coding

**If you find bugs during testing:**
1. You're already back on `develop` (step 4 above)
2. Fix the bug on `develop`
3. Commit and push (`git add .` → `git commit -m "fix"` → `git push`)
4. Merge into staging again (repeat the 4 commands above)

---

### 🚀 How to send code to RELEASE (Play Store build)

**Only do this when staging is fully tested and everything works perfectly.**

```bash
git checkout release
git merge staging
git push
git checkout develop
```

**What this does:**
1. Switches to the `release` branch
2. Copies the tested code from `staging` into `release`
3. Pushes it to GitHub
4. Switches you back to `develop`

Now you can build your Play Store APK/AAB from the `release` branch.

---

### 🆘 Common Situations

#### "I accidentally made changes on the wrong branch"
Don't panic. Run:
```bash
git stash
git checkout develop
git stash pop
```
This moves your uncommitted changes to `develop`.

#### "I want to see which branch I'm on"
```bash
git branch --show-current
```

#### "I want to see all my branches"
```bash
git branch
```
The one with the `*` is your current branch.

#### "I got a merge conflict"
This happens when the same line was changed in two branches. You'll see markers like:
```
<<<<<<< HEAD
code from current branch
=======
code from incoming branch
>>>>>>> develop
```
Pick the version you want, delete the markers, then:
```bash
git add .
git commit -m "resolved merge conflict"
git push
```

#### "I want to undo my last commit (haven't pushed yet)"
```bash
git reset --soft HEAD~1
```
Your changes stay in the files, but the commit is removed.

#### "I want to throw away all my uncommitted changes"
```bash
git checkout -- .
```
⚠️ This permanently deletes all unsaved changes!

---

## 🔄 Workflow Summary (Visual)

```
You write code
     │
     ▼
  develop  ──── git add . / git commit / git push
     │
     │  (feature ready? merge down ↓)
     ▼
  staging  ──── git checkout staging → git merge develop → git push
     │
     │  (tested & perfect? merge down ↓)
     ▼
  release  ──── git checkout release → git merge staging → git push
     │
     ▼
  Build APK → Upload to Play Store
```

---

## ✅ Quick Copy-Paste Commands

**Save work:**
```bash
git add . && git commit -m "your message" && git push
```

**Send to staging:**
```bash
git checkout staging && git merge develop && git push && git checkout develop
```

**Send to release:**
```bash
git checkout release && git merge staging && git push && git checkout develop
```
