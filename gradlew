#!/bin/sh

#
# 版权所有 © 2015-2021 原作者。
#
# 依据 Apache 许可证 2.0 版（“许可证”）进行许可；
# 除非遵守许可证，否则你不得使用此文件。
# 你可以在以下地址获取许可证副本：
#
#      https://www.apache.org/licenses/LICENSE-2.0
#
# 除非适用法律要求或书面同意，否则根据许可证分发的软件
# 均基于“按原样”分发，不附带任何形式的明示或默示担保。
# 请参阅许可证以了解管理权限和限制的具体语言。
#

##############################################################################
#
#   由 Gradle 生成的、适用于 POSIX 系统的 Gradle 启动脚本。
#
#   运行的重要提示：
#
#   (1) 你需要一个兼容 POSIX 的 shell 来运行此脚本。如果你的 /bin/sh
#       不兼容，但你拥有其他兼容的 shell，例如 ksh 或 bash，
#       那么要运行此脚本，请在整个命令行前键入该 shell 名称，例如：
#
#           ksh Gradle
#
#       Busybox 及类似的精简 shell 将无法工作，因为此脚本
#       需要以下所有 POSIX shell 特性：
#         * 函数（functions）；
#         * 展开式 «$var»、«${var}»、«${var:-default}»、«${var+SET}»、
#           «${var#prefix}»、«${var%suffix}» 和 «$( cmd )»；
#         * 具有可测试退出状态的复合命令，尤其是 «case»；
#         * 各种内建命令，包括 «command»、«set» 和 «ulimit»。
#
#   打补丁的重要提示：
#
#   (2) 此脚本面向任何 POSIX shell，因此它避免了 Bash、Ksh 等提供的扩展；
#       特别地，避免了数组。
#
#       将多个参数打包进一个以空格分隔的字符串的“传统”做法是
#       已知的 bug 和安全问题的来源，因此这里通过逐步在 "$@" 中
#       累积选项，并最终将其传递给 Java，来（大部分）避免此做法。
#
#       当继承的环境变量（DEFAULT_JVM_OPTS、JAVA_OPTS 和 GRADLE_OPTS）
#       依赖单词拆分时，此操作会显式执行；详情请见行内注释。
#
#       针对特定操作系统（如 AIX、CygWin、Darwin、MinGW 和 NonStop）有调整。
#
#   (3) 此脚本由 Gradle 项目中的 Groovy 模板生成：
#       https://github.com/gradle/gradle/blob/HEAD/subprojects/plugins/src/main/resources/org/gradle/api/internal/plugins/unixStartScript.txt
#
#       你可以在 https://github.com/gradle/gradle/ 找到 Gradle。
#
##############################################################################

# 尝试设置 APP_HOME

# 解析链接：$0 可能是一个链接
app_path=$0

# 处理菊花链式符号链接需要此循环。
while
    APP_HOME=${app_path%"${app_path##*/}"}  # 保留尾部 /；如果没有前置路径则为空
    [ -h "$app_path" ]
do
    ls=$( ls -ld "$app_path" )
    link=${ls#*' -> '}
    case $link in             #(
      /*)   app_path=$link ;; #(
      *)    app_path=$APP_HOME$link ;;
    esac
done

# 这通常未被使用
# shellcheck disable=SC2034
APP_BASE_NAME=${0##*/}
APP_HOME=$( cd "${APP_HOME:-./}" && pwd -P ) || exit

# 使用最大可用值，或设置 MAX_FD != -1 以使用该值。
MAX_FD=maximum

warn () {
    echo "$*"
} >&2

die () {
    echo
    echo "$*"
    echo
    exit 1
} >&2

# 操作系统特定支持（必须是 'true' 或 'false'）。
cygwin=false
msys=false
darwin=false
nonstop=false
case "$( uname )" in                #(
  CYGWIN* )         cygwin=true  ;; #(
  Darwin* )         darwin=true  ;; #(
  MSYS* | MINGW* )  msys=true    ;; #(
  NONSTOP* )        nonstop=true ;;
esac

CLASSPATH=$APP_HOME/gradle/wrapper/gradle-wrapper.jar


# 确定用于启动 JVM 的 Java 命令。
if [ -n "$JAVA_HOME" ] ; then
    if [ -x "$JAVA_HOME/jre/sh/java" ] ; then
        # IBM 在 AIX 上的 JDK 使用奇怪的可执行文件位置
        JAVACMD=$JAVA_HOME/jre/sh/java
    else
        JAVACMD=$JAVA_HOME/bin/java
    fi
    if [ ! -x "$JAVACMD" ] ; then
        die "错误：JAVA_HOME 指向了一个无效目录：$JAVA_HOME

请在你的环境中设置 JAVA_HOME 变量，使其指向
你的 Java 安装位置。"
    fi
else
    JAVACMD=java
    which java >/dev/null 2>&1 || die "错误：未设置 JAVA_HOME，并且在 PATH 中找不到 'java' 命令。

请在你的环境中设置 JAVA_HOME 变量，使其指向
你的 Java 安装位置。"
fi

# 尽可能增加最大文件描述符限制。
if ! "$cygwin" && ! "$darwin" && ! "$nonstop" ; then
    case $MAX_FD in #(
      max*)
        # 在 POSIX sh 中，ulimit -H 是未定义的。这就是为什么需要检查结果以确认它是否有效。
        # shellcheck disable=SC3045
        MAX_FD=$( ulimit -H -n ) ||
            warn "无法查询最大文件描述符限制"
    esac
    case $MAX_FD in  #(
      '' | soft) :;; #(
      *)
        # 在 POSIX sh 中，ulimit -n 是未定义的。这就是为什么需要检查结果以确认它是否有效。
        # shellcheck disable=SC3045
        ulimit -n "$MAX_FD" ||
            warn "无法将最大文件描述符限制设置为 $MAX_FD"
    esac
fi

# 收集 java 命令的所有参数，按逆序堆叠：
#   * 命令行参数
#   * 主类名
#   * -classpath
#   * -D...appname 设置
#   * --module-path（仅在需要时）
#   * 环境变量 DEFAULT_JVM_OPTS、JAVA_OPTS 和 GRADLE_OPTS。

# 对于 Cygwin 或 MSYS，在运行 java 之前将路径切换为 Windows 格式
if "$cygwin" || "$msys" ; then
    APP_HOME=$( cygpath --path --mixed "$APP_HOME" )
    CLASSPATH=$( cygpath --path --mixed "$CLASSPATH" )

    JAVACMD=$( cygpath --unix "$JAVACMD" )

    # 现在转换参数 - 限制我们仅使用 /bin/sh 的临时方案
    for arg do
        if
            case $arg in                                #(
              -*)   false ;;                            # 不要改动选项 #(
              /?*)  t=${arg#/} t=/${t%%/*}              # 看起来像一个 POSIX 文件路径
                    [ -e "$t" ] ;;                      #(
              *)    false ;;
            esac
        then
            arg=$( cygpath --path --ignore --mixed "$arg" )
        fi
        # 将参数列表恰好滚动与参数数量相同的次数，
        # 这样每个参数最终都会回到它开始的位置，但可能已被修改。
        #
        # 注意：`for` 循环在开始前捕获其迭代列表，因此
        # 此处更改位置参数既不影响迭代次数，也不影响 `arg` 中显示的值。
        shift                   # 移除旧参数
        set -- "$@" "$arg"      # 压入替换后的参数
    done
fi


# 在此处添加默认 JVM 选项。你也可以使用 JAVA_OPTS 和 GRADLE_OPTS 向此脚本传递 JVM 选项。
DEFAULT_JVM_OPTS='"-Xmx64m" "-Xms64m"'

# 收集 java 命令的所有参数；
#   * $DEFAULT_JVM_OPTS、$JAVA_OPTS 和 $GRADLE_OPTS 可能包含
#     带有引号和变量替换的 shell 脚本片段，因此将它们放在
#     双引号中以确保它们被重新展开；并且
#   * 将其他所有内容放在单引号中，以确保它不被重新展开。

set -- \
        "-Dorg.gradle.appname=$APP_BASE_NAME" \
        -classpath "$CLASSPATH" \
        org.gradle.wrapper.GradleWrapperMain \
        "$@"

# 当 "xargs" 不可用时停止。
if ! command -v xargs >/dev/null 2>&1
then
    die "xargs 不可用"
fi

# 使用 "xargs" 解析带引号的参数。
#
# 使用 -n1 选项，它每行输出一个参数，并移除引号和反斜杠。
#
# 在 Bash 中我们可以简单地执行：
#
#   readarray ARGS < <( xargs -n1 <<<"$var" ) &&
#   set -- "${ARGS[@]}" "$@"
#
# 但 POSIX shell 既没有数组也没有命令替换，因此我们
# 后处理每个参数（作为 sed 的一行输入），对任何可能是
# shell 元字符的字符进行反斜杠转义，然后使用 eval 反转
# 该过程（同时保持参数之间的分隔），并将整个
# 内容包装为单个 "set" 语句。
#
# 当然，如果这些变量中的任何一个包含换行符或不匹配的引号，
# 这将会失败。
#

eval "set -- $(
        printf '%s\n' "$DEFAULT_JVM_OPTS $JAVA_OPTS $GRADLE_OPTS" |
        xargs -n1 |
        sed ' s~[^-[:alnum:]+,./:=@_]~\\&~g; ' |
        tr '\n' ' '
    )" '"$@"'

exec "$JAVACMD" "$@"