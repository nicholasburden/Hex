package moveordering
import hexagony._

class MoveOrdering extends Const{
  var currentMoves : Set[Cell] = Set()
  var goodMoves : Set[Cell] = Set[Cell]()
  def initial(model : Model) = {
    for(move <- model.myCells(R) ++ model.myCells(B)){
      currentMoves = currentMoves + move
      for(goodMove <- getBridges(move, model) ++ model.neighbours(move)){
        if(goodMove.colour == O) goodMoves = goodMoves + goodMove

      }
    }
  }
  def getOrdering(model : Model) : List[Cell] = {
    var ordering : List[Cell] = List()
    if(model.count == 0){
      for(layer <- 0 until model.N/2){
        for(i <- layer until (model.N-layer)){
          ordering = model.board(i)(model.N-layer-1) :: model.board(i)(layer) :: ordering

        }
        for(j <- layer + 1 until (model.N-layer-1)){
          ordering = model.board(model.N-layer-1)(j) :: model.board(layer)(j) :: ordering
        }
      }
      if(model.N % 2 != 0){
        ordering = model.board(model.N/2)(model.N/2) :: ordering
      }
    }
    else{
      for(cell <- model.myCells(O)){
        if(!goodMoves.contains(cell) && !currentMoves.contains(cell)){
          ordering = cell :: ordering
        }
      }

      return goodMoves.toList ++ ordering
    }

    ordering
  }

  def addGoodMove(cell : Cell) = goodMoves = goodMoves + cell

  def addMovesFor(start : Cell, model : Model) : MoveOrdering = {
    val result = new MoveOrdering
    result.goodMoves = goodMoves
    result.currentMoves = currentMoves + start
    getBridges(start, model).foreach(x => result.addGoodMove(model.board(x.i)(x.j)))

    result.goodMoves = result.goodMoves -- result.currentMoves
    result
  }

  def isValid(i : Int, j : Int, model : Model) : Boolean = {
    return i >= 0 && i < model.N && j >= 0 && j < model.N && model.board(i)(j).colour == O
  }
  def getBridges(start : Cell, model : Model) : List[Cell] = {
    var list : List[Cell] = List()
    if(isValid(start.i-1, start.j-2, model)){
      list = model.board(start.i-1)(start.j-2) :: list
    }
    if(isValid(start.i-2, start.j-1, model)){
      list = model.board(start.i-2)(start.j-1) :: list
    }
    if(isValid(start.i-1, start.j+1, model)){
      list = model.board(start.i-1)(start.j+1) :: list
    }
    if(isValid(start.i+1, start.j+2, model)){
      list = model.board(start.i+1)(start.j+2) :: list
    }
    if(isValid(start.i+2, start.j+1, model)){
      list = model.board(start.i+2)(start.j+1) :: list
    }
    if(isValid(start.i+1, start.j-1, model)){
      list = model.board(start.i+1)(start.j-1) :: list
    }
    list
  }

}