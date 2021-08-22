package es.weso.pschema 

import org.apache.spark.sql.types._
import org.apache.spark.sql.Row
import org.apache.spark.sql.functions._
import com.github.mrpowers.spark.fast.tests._
import munit._
import es.weso.sparkwdsub.SampleSchemas
import es.weso.sparkwdsub.Helpers._
import es.weso.pschema.GraphBuilder._
import org.apache.spark.graphx.VertexRDD

class PSchemaSuite extends FunSuite 
  with SparkSessionTestWrapper with DatasetComparer with RDDComparer {

  import spark.implicits._

  // This test comes from spark-fast-tests README
  test("aliases a DataFrame") {
    val sourceDF = Seq(("jose"),("li"),("luisa")).toDF("name")
    val actualDF = sourceDF.select(col("name").alias("student"))
    val expectedDF = Seq(("jose"),("li"),("luisa")).toDF("student")
    assertSmallDatasetEquality(actualDF, expectedDF)
  }

  test("simple test about RDD") {
    val sourceRDD = spark.sparkContext.parallelize(Seq(("jose"),("li"))).map(_.toUpperCase())
    val expectedRDD = spark.sparkContext.parallelize(Seq(("JOSE"),("LI")))
    assertSmallRDDEquality(sourceRDD, expectedRDD)
  }

  test("Simple graph") {
     val graph = buildGraph(SampleSchemas.simpleGraph1, spark.sparkContext)
     val schema = SampleSchemas.schemaSimple
     val validatedGraph = PSchema[Value,Property,ShapeLabel,Reason, PropertyId](
        graph, ShapeLabel("Start"), 5)(
          schema.checkLocal,schema.checkNeighs,schema.getTripleConstraints,_.id
        )
    val vertices: List[(Long,ShapedValue[Value,ShapeLabel,Reason,PropertyId])] = 
        validatedGraph.vertices.collect().toList
    assertEquals(vertices.size,3)
    println(s"Vertices ${vertices.map(_.toString).mkString("\n")}")
    val maybeQ80 = vertices.map(_._2).collect { case sv => sv.value match { case e: Entity if e.id == "Q80" => sv }}.headOption
     maybeQ80 match {
         case None => fail(s"Not found value Q80")
         case Some(sv) => assertEquals(sv.shapesInfo.okShapes.map(_.name), Set("Human")) 
     }
  }

}