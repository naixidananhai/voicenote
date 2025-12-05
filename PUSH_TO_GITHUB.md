# 推送代码到GitHub指南

## 🔐 需要GitHub身份验证

推送代码前需要配置GitHub认证。有两种方式：

---

## ✨ 方式1: 使用GitHub Personal Access Token (推荐)

### 步骤1: 生成Personal Access Token

1. 访问GitHub网站：https://github.com/settings/tokens
2. 点击 **"Generate new token"** → **"Generate new token (classic)"**
3. 设置Token信息：
   - **Note**: 填写备注，如 "VoiceNote Development"
   - **Expiration**: 选择有效期（建议30天或更长）
   - **Select scopes**: 勾选以下权限：
     - ✅ **repo** (完整仓库权限)
     - ✅ **workflow** (GitHub Actions权限)
4. 点击底部 **"Generate token"**
5. **重要**: 复制生成的token（只显示一次！）

示例token格式：`ghp_xxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxx`

### 步骤2: 使用Token推送代码

打开命令行，执行：

```bash
cd .worktrees/voice-assistant-impl

# 推送代码（会提示输入用户名和密码）
git push -u origin feature/voice-assistant-impl
```

**输入提示时**：
- **Username**: 输入您的GitHub用户名 `yangyayuan`
- **Password**: **粘贴刚才复制的Token**（不是GitHub密码！）

---

## 🔑 方式2: 使用SSH密钥 (一次配置，长期使用)

### 步骤1: 生成SSH密钥

```bash
# 生成SSH密钥
ssh-keygen -t ed25519 -C "your_email@example.com"

# 按提示操作：
# - 文件位置：直接回车使用默认 (~/.ssh/id_ed25519)
# - 密码：可以留空（直接回车）或设置密码

# 查看公钥内容
cat ~/.ssh/id_ed25519.pub
# 或Windows: type %USERPROFILE%\.ssh\id_ed25519.pub
```

### 步骤2: 添加SSH公钥到GitHub

1. 复制刚才显示的公钥内容（以`ssh-ed25519`开头）
2. 访问：https://github.com/settings/ssh/new
3. **Title**: 填写名称，如 "My PC"
4. **Key**: 粘贴公钥内容
5. 点击 **"Add SSH key"**

### 步骤3: 修改远程仓库地址为SSH

```bash
cd .worktrees/voice-assistant-impl

# 查看当前远程地址
git remote -v

# 修改为SSH地址
git remote set-url origin git@github.com:yangyayuan/vocienote.git

# 推送代码
git push -u origin feature/voice-assistant-impl
```

---

## 🚀 快速推送流程（使用Token）

### 一步步操作：

1️⃣ **获取Token**
```
访问: https://github.com/settings/tokens/new
勾选: repo + workflow
生成并复制Token
```

2️⃣ **推送代码**
```bash
cd .worktrees/voice-assistant-impl
git push -u origin feature/voice-assistant-impl
```

3️⃣ **输入认证**
```
Username: yangyayuan
Password: [粘贴Token]
```

4️⃣ **等待完成**
```
推送约需30秒-2分钟
成功后会显示分支推送信息
```

---

## 📋 完整推送命令

```bash
# 进入项目目录
cd .worktrees/voice-assistant-impl

# 确认远程仓库已添加
git remote -v
# 应该显示: origin  https://github.com/yangyayuan/vocienote.git

# 查看当前分支
git branch
# 应该显示: * feature/voice-assistant-impl

# 查看待推送的提交
git log --oneline -5

# 推送代码
git push -u origin feature/voice-assistant-impl
# 输入用户名和Token

# 推送成功后，也推送master分支（如果需要）
git checkout master
git push -u origin master
```

---

## ✅ 推送后自动触发编译

推送成功后，GitHub Actions会自动开始编译！

### 查看编译进度：

1. 访问仓库页面：https://github.com/yangyayuan/vocienote
2. 点击 **"Actions"** 标签
3. 看到 "Android CI - Build APK" 工作流运行中
4. 等待5-10分钟编译完成（绿色✓）
5. 下载编译好的APK文件

---

## 🔍 验证推送成功

推送成功后，可以验证：

```bash
# 查看远程分支
git branch -r

# 查看推送状态
git status

# 查看远程提交
git log origin/feature/voice-assistant-impl --oneline -5
```

或访问GitHub网页：
```
https://github.com/yangyayuan/vocienote/tree/feature/voice-assistant-impl
```

---

## ❓ 常见问题

### Q1: 提示"Permission denied"
**A**: Token权限不足，重新生成时确保勾选了 `repo` 权限。

### Q2: 提示"Authentication failed"
**A**:
- 检查用户名是否正确
- 确保粘贴的是Token（不是GitHub密码）
- Token可能已过期，重新生成

### Q3: 推送很慢或卡住
**A**:
- 检查网络连接
- 项目较大，首次推送需要时间
- 可能被防火墙阻止，尝试使用代理

### Q4: 提示"fatal: refusing to merge unrelated histories"
**A**:
```bash
# 如果GitHub仓库已有内容，需要先拉取
git pull origin feature/voice-assistant-impl --allow-unrelated-histories
git push -u origin feature/voice-assistant-impl
```

### Q5: 想保存Token避免每次输入
**A**: 配置凭据存储
```bash
# Linux/Mac
git config --global credential.helper store

# Windows
git config --global credential.helper wincred

# 下次推送后会自动保存
```

---

## 💡 推荐方式

**对于初次使用**：
✅ 使用 Personal Access Token
- 简单快速
- 无需SSH配置
- 适合临时使用

**对于长期开发**：
✅ 配置SSH密钥
- 一次配置永久使用
- 无需每次输入密码
- 更安全便捷

---

## 🎯 我应该怎么做？

**最简单的方式：**

1. 生成Token：https://github.com/settings/tokens/new
2. 勾选 `repo` 和 `workflow`
3. 复制Token
4. 执行命令：
   ```bash
   cd .worktrees/voice-assistant-impl
   git push -u origin feature/voice-assistant-impl
   ```
5. 输入用户名 `yangyayuan` 和Token
6. 等待推送完成
7. 访问GitHub查看Actions自动编译

**就这么简单！** 🚀
