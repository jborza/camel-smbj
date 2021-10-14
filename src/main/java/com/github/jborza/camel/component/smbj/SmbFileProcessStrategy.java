package com.github.jborza.camel.component.smbj;

import org.apache.camel.CamelContext;
import org.apache.camel.Expression;
import org.apache.camel.LoggingLevel;
import org.apache.camel.component.file.GenericFileExclusiveReadLockStrategy;
import org.apache.camel.component.file.GenericFileProcessStrategy;
import org.apache.camel.component.file.GenericFileProcessStrategyFactory;
import org.apache.camel.component.file.strategy.FileChangedExclusiveReadLockStrategy;
import org.apache.camel.component.file.strategy.FileIdempotentChangedRepositoryReadLockStrategy;
import org.apache.camel.component.file.strategy.FileIdempotentRenameRepositoryReadLockStrategy;
import org.apache.camel.component.file.strategy.FileIdempotentRepositoryReadLockStrategy;
import org.apache.camel.component.file.strategy.FileLockExclusiveReadLockStrategy;
import org.apache.camel.component.file.strategy.FileRenameExclusiveReadLockStrategy;
import org.apache.camel.component.file.strategy.GenericFileDeleteProcessStrategy;
import org.apache.camel.component.file.strategy.GenericFileExpressionRenamer;
import org.apache.camel.component.file.strategy.GenericFileRenameProcessStrategy;
import org.apache.camel.component.file.strategy.MarkerFileExclusiveReadLockStrategy;
import org.apache.camel.spi.IdempotentRepository;
import org.apache.camel.spi.Language;
import org.apache.camel.util.ObjectHelper;

import java.io.File;
import java.util.Map;
import java.util.concurrent.ScheduledExecutorService;

public class SmbFileProcessStrategy implements GenericFileProcessStrategyFactory<SmbFile> {

    public GenericFileProcessStrategy<SmbFile> createGenericFileProcessStrategy(CamelContext context, Map<String, Object> params) {
        Expression moveExpression = (Expression)params.get("move");
        Expression moveFailedExpression = (Expression)params.get("moveFailed");
        Expression preMoveExpression = (Expression)params.get("preMove");
        boolean isNoop = params.get("noop") != null;
        boolean isDelete = params.get("delete") != null;
        boolean isMove = moveExpression != null || preMoveExpression != null || moveFailedExpression != null;
        GenericFileExpressionRenamer renamer;
        if (isDelete) {
            GenericFileDeleteProcessStrategy<SmbFile> strategy = new GenericFileDeleteProcessStrategy();
            strategy.setExclusiveReadLockStrategy(getExclusiveReadLockStrategy(params));
            if (preMoveExpression != null) {
                renamer = new GenericFileExpressionRenamer();
                renamer.setExpression(preMoveExpression);
                strategy.setBeginRenamer(renamer);
            }

            if (moveFailedExpression != null) {
                renamer = new GenericFileExpressionRenamer();
                renamer.setExpression(moveFailedExpression);
                strategy.setFailureRenamer(renamer);
            }

            return strategy;
        } else {
            GenericFileRenameProcessStrategy strategy;
            if (!isMove && !isNoop) {
                strategy = new GenericFileRenameProcessStrategy();
                strategy.setExclusiveReadLockStrategy(getExclusiveReadLockStrategy(params));
                strategy.setCommitRenamer(getDefaultCommitRenamer(context));
                return strategy;
            } else {
                strategy = new GenericFileRenameProcessStrategy();
                strategy.setExclusiveReadLockStrategy(getExclusiveReadLockStrategy(params));
                if (!isNoop) {
                    if (moveExpression != null) {
                        renamer = new GenericFileExpressionRenamer();
                        renamer.setExpression(moveExpression);
                        strategy.setCommitRenamer(renamer);
                    } else {
                        strategy.setCommitRenamer(getDefaultCommitRenamer(context));
                    }
                }

                if (preMoveExpression != null) {
                    renamer = new GenericFileExpressionRenamer();
                    renamer.setExpression(preMoveExpression);
                    strategy.setBeginRenamer(renamer);
                }

                if (moveFailedExpression != null) {
                    renamer = new GenericFileExpressionRenamer();
                    renamer.setExpression(moveFailedExpression);
                    strategy.setFailureRenamer(renamer);
                }

                return strategy;
            }
        }
    }

    private static GenericFileExpressionRenamer<File> getDefaultCommitRenamer(CamelContext context) {
        Language language = context.resolveLanguage("file");
        Expression expression = language.createExpression("${file:parent}/.camel/${file:onlyname}");
        return new GenericFileExpressionRenamer(expression);
    }

    private static GenericFileExclusiveReadLockStrategy<SmbFile> getExclusiveReadLockStrategy(Map<String, Object> params) {
        GenericFileExclusiveReadLockStrategy<File> strategy = (GenericFileExclusiveReadLockStrategy)params.get("exclusiveReadLockStrategy");
        if (strategy != null) {
            return (GenericFileExclusiveReadLockStrategy)strategy;
        } else {
            String readLock = (String)params.get("readLock");
            if (ObjectHelper.isNotEmpty(readLock)) {
                if ("none".equals(readLock) || "false".equals(readLock)) {
                    return null;
                }

                Long minLength;
                if ("markerFile".equals(readLock)) {
                    strategy = new MarkerFileExclusiveReadLockStrategy();
                } else if ("fileLock".equals(readLock)) {
                    strategy = new FileLockExclusiveReadLockStrategy();
                } else if ("rename".equals(readLock)) {
                    strategy = new FileRenameExclusiveReadLockStrategy();
                } else if ("changed".equals(readLock)) {
                    FileChangedExclusiveReadLockStrategy readLockStrategy = new FileChangedExclusiveReadLockStrategy();
                    minLength = (Long)params.get("readLockMinLength");
                    if (minLength != null) {
                        readLockStrategy.setMinLength(minLength);
                    }

                    Long minAge = (Long)params.get("readLockMinAge");
                    if (null != minAge) {
                        readLockStrategy.setMinAge(minAge);
                    }

                    strategy = readLockStrategy;
                } else {
                    IdempotentRepository repo;
                    Integer readLockIdempotentReleaseDelay;
                    Boolean readLockRemoveOnRollback;
                    Boolean readLockRemoveOnCommit;
                    if ("idempotent".equals(readLock)) {
                        FileIdempotentRepositoryReadLockStrategy readLockStrategy = new FileIdempotentRepositoryReadLockStrategy();
                        readLockRemoveOnRollback = (Boolean)params.get("readLockRemoveOnRollback");
                        if (readLockRemoveOnRollback != null) {
                            readLockStrategy.setRemoveOnRollback(readLockRemoveOnRollback);
                        }

                        readLockRemoveOnCommit = (Boolean)params.get("readLockRemoveOnCommit");
                        if (readLockRemoveOnCommit != null) {
                            readLockStrategy.setRemoveOnCommit(readLockRemoveOnCommit);
                        }

                        repo = (IdempotentRepository)params.get("readLockIdempotentRepository");
                        if (repo != null) {
                            readLockStrategy.setIdempotentRepository(repo);
                        }

                        readLockIdempotentReleaseDelay = (Integer)params.get("readLockIdempotentReleaseDelay");
                        if (readLockIdempotentReleaseDelay != null) {
                            readLockStrategy.setReadLockIdempotentReleaseDelay(readLockIdempotentReleaseDelay);
                        }

                        Boolean readLockIdempotentReleaseAsync = (Boolean)params.get("readLockIdempotentReleaseAsync");
                        if (readLockIdempotentReleaseAsync != null) {
                            readLockStrategy.setReadLockIdempotentReleaseAsync(readLockIdempotentReleaseAsync);
                        }

                        readLockIdempotentReleaseDelay = (Integer)params.get("readLockIdempotentReleaseAsyncPoolSize");
                        if (readLockIdempotentReleaseDelay != null) {
                            readLockStrategy.setReadLockIdempotentReleaseAsyncPoolSize(readLockIdempotentReleaseDelay);
                        }

                        ScheduledExecutorService readLockIdempotentReleaseExecutorService = (ScheduledExecutorService)params.get("readLockIdempotentReleaseExecutorService");
                        if (readLockIdempotentReleaseExecutorService != null) {
                            readLockStrategy.setReadLockIdempotentReleaseExecutorService(readLockIdempotentReleaseExecutorService);
                        }

                        strategy = readLockStrategy;
                    } else if ("idempotent-changed".equals(readLock)) {
                        FileIdempotentChangedRepositoryReadLockStrategy readLockStrategy = new FileIdempotentChangedRepositoryReadLockStrategy();
                        readLockRemoveOnRollback = (Boolean)params.get("readLockRemoveOnRollback");
                        if (readLockRemoveOnRollback != null) {
                            readLockStrategy.setRemoveOnRollback(readLockRemoveOnRollback);
                        }

                        readLockRemoveOnCommit = (Boolean)params.get("readLockRemoveOnCommit");
                        if (readLockRemoveOnCommit != null) {
                            readLockStrategy.setRemoveOnCommit(readLockRemoveOnCommit);
                        }

                        repo = (IdempotentRepository)params.get("readLockIdempotentRepository");
                        if (repo != null) {
                            readLockStrategy.setIdempotentRepository(repo);
                        }

                        minLength = (Long)params.get("readLockMinLength");
                        if (minLength != null) {
                            readLockStrategy.setMinLength(minLength);
                        }

                        Long minAge = (Long)params.get("readLockMinAge");
                        if (null != minAge) {
                            readLockStrategy.setMinAge(minAge);
                        }

                        readLockIdempotentReleaseDelay = (Integer)params.get("readLockIdempotentReleaseDelay");
                        if (readLockIdempotentReleaseDelay != null) {
                            readLockStrategy.setReadLockIdempotentReleaseDelay(readLockIdempotentReleaseDelay);
                        }

                        Boolean readLockIdempotentReleaseAsync = (Boolean)params.get("readLockIdempotentReleaseAsync");
                        if (readLockIdempotentReleaseAsync != null) {
                            readLockStrategy.setReadLockIdempotentReleaseAsync(readLockIdempotentReleaseAsync);
                        }

                        Integer readLockIdempotentReleaseAsyncPoolSize = (Integer)params.get("readLockIdempotentReleaseAsyncPoolSize");
                        if (readLockIdempotentReleaseAsyncPoolSize != null) {
                            readLockStrategy.setReadLockIdempotentReleaseAsyncPoolSize(readLockIdempotentReleaseAsyncPoolSize);
                        }

                        ScheduledExecutorService readLockIdempotentReleaseExecutorService = (ScheduledExecutorService)params.get("readLockIdempotentReleaseExecutorService");
                        if (readLockIdempotentReleaseExecutorService != null) {
                            readLockStrategy.setReadLockIdempotentReleaseExecutorService(readLockIdempotentReleaseExecutorService);
                        }

                        strategy = readLockStrategy;
                    } else if ("idempotent-rename".equals(readLock)) {
                        FileIdempotentRenameRepositoryReadLockStrategy readLockStrategy = new FileIdempotentRenameRepositoryReadLockStrategy();
                        readLockRemoveOnRollback = (Boolean)params.get("readLockRemoveOnRollback");
                        if (readLockRemoveOnRollback != null) {
                            readLockStrategy.setRemoveOnRollback(readLockRemoveOnRollback);
                        }

                        readLockRemoveOnCommit = (Boolean)params.get("readLockRemoveOnCommit");
                        if (readLockRemoveOnCommit != null) {
                            readLockStrategy.setRemoveOnCommit(readLockRemoveOnCommit);
                        }

                        repo = (IdempotentRepository)params.get("readLockIdempotentRepository");
                        if (repo != null) {
                            readLockStrategy.setIdempotentRepository(repo);
                        }

                        strategy = readLockStrategy;
                    }
                }

                if (strategy != null) {
                    Long timeout = (Long)params.get("readLockTimeout");
                    if (timeout != null) {
                        ((GenericFileExclusiveReadLockStrategy)strategy).setTimeout(timeout);
                    }

                    minLength = (Long)params.get("readLockCheckInterval");
                    if (minLength != null) {
                        ((GenericFileExclusiveReadLockStrategy)strategy).setCheckInterval(minLength);
                    }

                    LoggingLevel readLockLoggingLevel = (LoggingLevel)params.get("readLockLoggingLevel");
                    if (readLockLoggingLevel != null) {
                        ((GenericFileExclusiveReadLockStrategy)strategy).setReadLockLoggingLevel(readLockLoggingLevel);
                    }

                    Boolean readLockMarkerFile = (Boolean)params.get("readLockMarkerFile");
                    if (readLockMarkerFile != null) {
                        ((GenericFileExclusiveReadLockStrategy)strategy).setMarkerFiler(readLockMarkerFile);
                    }

                    Boolean readLockDeleteOrphanLockFiles = (Boolean)params.get("readLockDeleteOrphanLockFiles");
                    if (readLockDeleteOrphanLockFiles != null) {
                        ((GenericFileExclusiveReadLockStrategy)strategy).setDeleteOrphanLockFiles(readLockDeleteOrphanLockFiles);
                    }
                }
            }

            return (GenericFileExclusiveReadLockStrategy)strategy;
        }
    }
}
