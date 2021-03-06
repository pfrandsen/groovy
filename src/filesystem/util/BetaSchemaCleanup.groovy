package filesystem.util

import groovy.util.logging.*

import org.apache.commons.cli.Option

import java.util.logging.Level

@Log
class BetaSchemaCleanup {
    private static String productionSchemasRoot = "target/SchemaServer/prod_schemas"
    private static String betaSchemasRoot = "target/SchemaServer/beta_schemas"
    private static String[] folders = ["concept", "technical", "external"]

    private static void processArguments(def options) {
        if (options.p) {
            productionSchemasRoot = options.p
        }
        if (options.b) {
            betaSchemasRoot = options.b
        }
        if (options.f) {
            folders = options.fs // 's' needs to be added to get all values and not just the first
        }
    }

    private static String getCanonicalPath(String path) {
        return new File(path).canonicalPath
    }

    private static CliBuilder commandlineParser() {
        CliBuilder cli = new CliBuilder(usage: 'BetaSchemaCleanup [options]')
        cli.with {
            _(longOpt: 'help', 'Show usage information')
            p(longOpt: 'production', "Path to root of production schemas, default '${productionSchemasRoot}'", args: 1)
            b(longOpt: 'beta', "Path to root of beta schemas, default '${betaSchemasRoot}'", args: 1)
            f(longOpt: 'folders', "Beta schema folders to clean-up, default '${folders.join(',')}'", args: Option.UNLIMITED_VALUES, valueSeparator: ',')
        }
        return cli
    }

    public static void main(String[] args) {
        CliBuilder cli = commandlineParser()
        def options = cli.parse(args)
        if (!options) {
            return
        }
        if(options.help) {
            cli.usage()
            return
        }
        processArguments(options)
        CompareAndCleanup runner = new CompareAndCleanup(getCanonicalPath(productionSchemasRoot), getCanonicalPath(betaSchemasRoot), folders)
        runner.doCleanup()
        log.info("Deleted ${runner.foldersDeleted} folder${runner.foldersDeleted == 1 ? '' : 's'} of ${runner.cloneFolderCount} in ${betaSchemasRoot}[${folders.join(',')}].")
        log.info("Deleted ${runner.filesDeleted} file${runner.filesDeleted == 1 ? '' : 's'} of ${runner.cloneFileCount} in ${betaSchemasRoot}[${folders.join(',')}].")
        if (runner.deleteFailures > 0) {
            log.log(Level.WARNING, "${runner.deleteFailures} ${runner.deleteFailures == 1 ? 'file/folder' : 'files/folders'} could not be deleted.")
        }
    }
}
