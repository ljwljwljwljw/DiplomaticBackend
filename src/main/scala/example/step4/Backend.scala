package example.step4

import chipsalliance.rocketchip.config._
import chisel3._
import chisel3.util._
import freechips.rocketchip.diplomacy._
import freechips.rocketchip.util.HasRocketChipStageUtils

trait DontOmitGraphML { this: LazyModule =>
  override def omitGraphML: Boolean = false //this.omitGraphML
}

class ExuInput extends Bundle
class ExuOutput(val priority: Int) extends Bundle

class ExBlock(implicit p: Parameters) extends SimpleLazyModule with DontOmitGraphML {
  val rs = LazyModule(new ReservationStation())
  // create an additional hierarchy
  val exus = LazyScope("exu_wrapper", "ExuWrapper"){
    Seq(
      LazyModule(new Alu()).suggestName("alu"),
      LazyModule(new Mdu()).suggestName("mdu")
    )
  }
  for(exu <- exus){
    exu.input_node := rs.issue_node
  }
}

class Backend(implicit p: Parameters) extends SimpleLazyModule with DontOmitGraphML {

  val exBlocks = Seq.fill(2){ LazyModule(new ExBlock()) }
  val wb_arb = LazyModule(new WriteBackArbiter())

  for(block <- exBlocks){
    for(exu <- block.exus){
      /*
            wb_arb <--|
            rs_0   <--|<-- broadcast <-- exu.output
            rs_1   <--|
       */
      val broadCastNode = DecoupledBroadCast[ExuOutput]()
      broadCastNode := exu.output_node
      wb_arb.input_node := broadCastNode
      for(rs <- exBlocks.map(_.rs)) {
        rs.wakeup_node := broadCastNode
      }
    }
  }

}

class ReservationStation(implicit p: Parameters) extends SimpleLazyModule with DontOmitGraphML {
  val issue_node = BundleBridgeNexusNode(Some(() => Decoupled(new ExuInput)))
  val wakeup_node = BundleBridgeNexusNode[DecoupledIO[ExuOutput]]()
}

abstract class Exu(val priority: Int)(implicit p: Parameters) extends SimpleLazyModule with DontOmitGraphML {
  val input_node = BundleBridgeSink[DecoupledIO[ExuInput]]()
  val output_node = BundleBridgeSource(() => DecoupledIO(new ExuOutput(priority)))
}

class Alu(implicit p: Parameters) extends Exu(0)
class Mdu(implicit p: Parameters) extends Exu(1)

class WriteBackArbiter(implicit p: Parameters) extends LazyModule with DontOmitGraphML {
  val input_node = BundleBridgeNexusNode[DecoupledIO[ExuOutput]]()
  lazy val module = new LazyModuleImp(this){
    val inputs = input_node.in.map(_._1)
    val exclusive_ports = inputs.filter(_.bits.priority == 0)
    val shared_ports = inputs.filterNot(_.bits.priority != 0)
    exclusive_ports.foreach(_.ready := true.B)
    // ...
  }
}

class DecoupledBroadCast[T <: Data](implicit p: Parameters) extends LazyModule {
  val node = BundleBridgeNexusNode[DecoupledIO[T]]()
  lazy val module = new LazyModuleImp(this){
    require(node.in.size == 1)
    require(node.out.nonEmpty)
    val input = node.in.head._1
    val outputs = node.out.map(_._1)
    input.ready := Cat(outputs.map(_.ready)).andR()
    for(o <- outputs){
      o.valid := input.valid
      o.bits := input.bits
    }
  }
}

object DecoupledBroadCast {
  def apply[T <: Data]()(implicit p: Parameters): BundleBridgeNexusNode[DecoupledIO[T]] = {
    val decoupledBroadCast = LazyModule(new DecoupledBroadCast[T]())
    decoupledBroadCast.node
  }
}

object Backend extends App with HasRocketChipStageUtils {
  override def main(args: Array[String]): Unit = {
    implicit val config = Parameters.empty
    val backend = LazyModule(new Backend())
    val verilog = chisel3.stage.ChiselStage.emitVerilog(backend.module)
    writeOutputFile(".", "Backend.v", verilog)
    writeOutputFile(".", "Backend.graphml", backend.graphML)
  }
}