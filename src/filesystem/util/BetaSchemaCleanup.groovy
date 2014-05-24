package filesystem.util

import org.apache.commons.cli.Option

class BetaSchemaCleanup {
    private static String productionSchemasRoot = "../../../test/resource/SchemaServer/prod_schemas"
    private static String betaSchemasRoot = "../../../test/resource/SchemaServer/beta_schemas"
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
        CompareAndCleanup runner = new CompareAndCleanup(productionSchemasRoot, betaSchemasRoot, folders)
        runner.doCleanup()
        System.out.println("Deleted ${runner.filesDeleted} files and ${runner.foldersDeleted} folders.")
    }
}
