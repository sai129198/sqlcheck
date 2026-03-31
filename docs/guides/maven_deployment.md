# Maven 发布指南

## 发布选项

本项目支持发布到以下 Maven 仓库：

1. **Maven Central** (推荐) - 通过 OSSRH
2. **GitHub Packages** - GitHub 提供的 Maven 仓库
3. **私有仓库** - 如 Nexus、Artifactory 等

---

## 方法一：发布到 Maven Central（推荐）

### 1. 准备工作

#### 1.1 注册 JIRA 账号

访问 https://issues.sonatype.org/ 注册账号。

#### 1.2 创建项目工单

创建新工单申请仓库权限：
- Project: Community Support - Open Source Project Repository Hosting
- Issue Type: New Project
- Group Id: 你的 groupId (如 `com.example`)
- Project URL: GitHub 项目地址
- SCM URL: Git 仓库地址

#### 1.3 配置 GPG 密钥

```bash
# 生成 GPG 密钥
gpg --gen-key

# 列出密钥
gpg --list-keys

# 发布公钥到密钥服务器
gpg --keyserver keyserver.ubuntu.com --send-keys YOUR_KEY_ID
gpg --keyserver keys.openpgp.org --send-keys YOUR_KEY_ID
```

### 2. 配置 Maven

编辑 `~/.m2/settings.xml`：

```xml
<settings>
    <servers>
        <server>
            <id>ossrh</id>
            <username>你的JIRA用户名</username>
            <password>你的JIRA密码</password>
        </server>
    </servers>

    <profiles>
        <profile>
            <id>gpg</id>
            <properties>
                <gpg.executable>gpg</gpg.executable>
                <gpg.passphrase>你的GPG密码</gpg.passphrase>
                <gpg.keyname>你的GPG密钥ID</gpg.keyname>
            </properties>
        </profile>
    </profiles>
</settings>
```

### 3. 发布步骤

```bash
# 1. 确保所有测试通过
mvn clean test

# 2. 更新版本号（去掉 SNAPSHOT）
mvn versions:set -DnewVersion=1.0.0

# 3. 清理并打包
mvn clean package

# 4. 发布到 Maven Central
mvn clean deploy -P gpg

# 5. 创建 Git 标签
git tag -a v1.0.0 -m "Release version 1.0.0"
git push origin v1.0.0

# 6. 为下一个开发周期更新版本
mvn versions:set -DnewVersion=1.1.0-SNAPSHOT
```

---

## 方法二：发布到 GitHub Packages

### 1. 创建 GitHub Token

访问 https://github.com/settings/tokens 创建 Personal Access Token：
- 勾选 `write:packages` 权限
- 勾选 `read:packages` 权限

### 2. 配置 Maven

编辑 `~/.m2/settings.xml`：

```xml
<settings>
    <servers>
        <server>
            <id>github</id>
            <username>你的GitHub用户名</username>
            <password>你的GitHub Token</password>
        </server>
    </servers>
</settings>
```

### 3. 修改 pom.xml

取消 `pom.xml` 中 GitHub Packages 的注释：

```xml
<distributionManagement>
    <repository>
        <id>github</id>
        <name>GitHub Packages</name>
        <url>https://maven.pkg.github.com/sai129198/sqlcheck</url>
    </repository>
</distributionManagement>
```

### 4. 发布

```bash
mvn clean deploy
```

---

## 方法三：发布到私有仓库

### 配置私有仓库地址

```xml
<distributionManagement>
    <repository>
        <id>private-repo</id>
        <name>Private Repository</name>
        <url>http://your-nexus-server/repository/maven-releases/</url>
    </repository>
    <snapshotRepository>
        <id>private-repo</id>
        <name>Private Repository Snapshots</name>
        <url>http://your-nexus-server/repository/maven-snapshots/</url>
    </snapshotRepository>
</distributionManagement>
```

---

## 本地打包安装

如果你只想在本地使用，可以安装到本地 Maven 仓库：

```bash
# 安装到本地仓库
mvn clean install

# 跳过测试安装
mvn clean install -DskipTests
```

安装后，其他项目可以通过以下方式引用：

```xml
<dependency>
    <groupId>com.example</groupId>
    <artifactId>sql-parser</artifactId>
    <version>1.0.0-SNAPSHOT</version>
</dependency>
```

---

## 打包命令速查

```bash
# 基础打包
mvn clean package

# 打包并生成源码包和文档包
mvn clean package -P release

# 仅生成源码包
mvn clean source:jar

# 仅生成文档包
mvn clean javadoc:jar

# 查看打包内容
jar tf target/sql-parser-1.0.0-SNAPSHOT.jar
```

---

## 常见问题

### Q: GPG 签名失败

A: 确保 GPG 密钥已发布到密钥服务器，并在 `settings.xml` 中正确配置。

### Q: 发布到 Maven Central 被拒绝

A: 检查：
1. 是否有正确的 groupId 权限
2. 是否包含必要的元数据（license、developers、scm）
3. 是否使用 GPG 签名
4. 是否包含源码包和文档包

### Q: 如何发布 SNAPSHOT 版本？

A: 版本号以 `-SNAPSHOT` 结尾时，会自动发布到快照仓库：

```bash
mvn clean deploy
```

### Q: 如何跳过 GPG 签名？

A: 对于 GitHub Packages 或私有仓库，可以跳过 GPG：

```bash
mvn clean deploy -Dgpg.skip=true
```

---

## 参考文档

- [Maven Central 发布指南](https://central.sonatype.org/publish/publish-guide/)
- [GitHub Packages 文档](https://docs.github.com/en/packages/working-with-a-github-packages-registry/working-with-the-apache-maven-registry)
- [Maven 部署文档](https://maven.apache.org/guides/mini/guide-deployment.html)
