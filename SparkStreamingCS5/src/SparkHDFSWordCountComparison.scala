import java.io.File

import org.apache.spark.{ SparkConf, SparkContext }

import scala.io.Source._

import org.apache.log4j.{ Level, Logger }

object SparkHDFSWordCountComparison {

  // defining the local file directory

  private var localFilePath: File = new File("/home/acadgild/Desktop/Spark_Streaming/text")

  //defining the directory in hdfs path

  private var dfsDirPath: String = "hdfs://localhost:8020/user"

  private val NPARAMS = 2

  def main(args: Array[String]): Unit = {

    //parseArgs(args)

    println("SparkHDFSWordCountComparison : Main Called Successfully")

    println("Performing local word count")

    //read the file which is present in local directory and convert into string

    val fileContents = readFile(localFilePath.toString())

    println("Performing local word count - File Content ->>" + fileContents)

    val localWordCount = runLocalWordCount(fileContents)

    println("SparkHDFSWordCountComparison : Main Called Successfully -> Local Word Count is - >>" + localWordCount)

    println("Performing local word count Completed !!")

    println("Creating Spark Context")

    //Create spark context

    val conf = new SparkConf().setMaster("local[2]").setAppName("SparkHDFSWordCountComparisonApp")

    val sc = new SparkContext(conf)

    // Setting log level to [WARN] for streaming executions and to override add a custom log4j.properties to the

    //classpath

    val rootLogger = Logger.getRootLogger()

    rootLogger.setLevel(Level.ERROR)

    println("Spark Context Created")

    println("Writing local file to DFS")

    val dfsFilename = dfsDirPath + "/dfs_read_write_test"

    val fileRDD = sc.parallelize(fileContents)

    fileRDD.saveAsTextFile(dfsFilename)

    println("Writing local file to DFS Completed")

    println("Reading file from DFS and running Word Count")

    val readFileRDD = sc.textFile(dfsFilename)

    val dfsWordCount = readFileRDD

      .flatMap(_.split(" "))

      .flatMap(_.split("\t"))

      .filter(_.nonEmpty)

      .map(w => (w, 1))

      .countByKey()

      .values

      .sum

    sc.stop()

    //apply if condition to check word count result from both the directories

    if (localWordCount == dfsWordCount) {

      println(s"Success! Local Word Count ($localWordCount) " +

        s"and DFS Word Count ($dfsWordCount) agree.")

    } else {

      println(s"Failure! Local Word Count ($localWordCount)"

        + s"and DFS Word Count ($dfsWordCount) disagree.")

    }

  }

/***private def parseArgs(args: Array[String]): Unit =

{ if (args.length != NPARAMS) {

printUsage()

System.exit(1)

}

}***/

  private def printUsage(): Unit = {

    val usage: String = "DFS Read-Write Test\n"+
    "\n"+
    "Usage: localFile dfsDir\n"+
    "\n"+
    "localFile - (string) local file to use in test\n" +
    "dfsDir - (string) DFS directory for read/write tests\n"

    println(usage)

  }

  private def readFile(filename: String): List[String] = {

    val lineIter: Iterator[String] = fromFile(filename).getLines()

    val lineList: List[String] = lineIter.toList

    lineList

  }

  def runLocalWordCount(fileContents: List[String]): Int =

    {

      fileContents.flatMap(_.split(" "))

        .flatMap(_.split("\t"))

        .filter(_.nonEmpty)

        .groupBy(w => w)

        .mapValues(_.size)

        .values

        .sum

    }

}