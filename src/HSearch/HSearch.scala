package HSearch

import hexagony._

class HSearch(val model: Model, colour: Colour) extends Const{



  var Gtemp : Set[Cell] = Set()
  for(cell <- model.myCells(colour)){
    Gtemp = Gtemp + cell
  }
  for(cell <- model.myCells(O)){
    Gtemp = Gtemp + cell
  }
  if(colour == B){
    Gtemp = Gtemp + HSearch.boundaryBlue1
    Gtemp = Gtemp + HSearch.boundaryBlue2
  }
  else{
    Gtemp = Gtemp + HSearch.boundaryRed1
    Gtemp = Gtemp + HSearch.boundaryRed2
  }
  var G : DisjointSets[Cell] = new DisjointSets[Cell]()
  for(g <- Gtemp){

    G.add(g)
  }

  for(g1 <- Gtemp){

    for(g2 <- Gtemp){
      if(g1.colour.equals(colour) && g2.colour.equals(colour) && areNearestNeighbours(g1, g2)){

        G.union(g1, g2)
      }
    }
  }

  var C : collection.mutable.Map[(Cell, Cell), Set[Set[Cell]]] = collection.mutable.Map()
  var SC : collection.mutable.Map[(Cell, Cell), Set[Set[Cell]]] = collection.mutable.Map()
  var oldC : collection.mutable.Map[(Cell, Cell), Set[Set[Cell]]] = collection.mutable.Map()

  for(g1 <- G.getReps){
    for(g2 <- G.getReps){
      oldC((g1, g2)) = Set()
    }
  }
  def initial = {

    for(g1 <- G.getReps){

      for(g2 <- G.getReps){

        if(!areNearestNeighbours(g1,g2)){
          C((g1, g2)) = Set()
        }
        else{
          C((g1, g2)) = Set(Set())
        }
        SC((g1, g2)) = Set()

      }
    }
  }
  def makeMove(i : Int, j : Int, c : Colour): HSearch = {
    val cell = model.board(i)(j)
    val mod2 = result(model, cell, c)
    val hsearch = new HSearch(mod2, colour)
    val C_clone = clone(C, hsearch)
    val SC_clone = clone(SC, hsearch)
    hsearch.C = C_clone
    hsearch.SC = SC_clone
    for(cell1 <- model.myCells(colour) ++ model.myCells(O)){
      for(cell2 <- model.myCells(colour) ++ model.myCells(O)){
        val strongCarriers = getStrongCarriers(cell1, cell2, true)
        if(strongCarriers.contains(cell) && !c.equals(colour)){
          hsearch.SC((hsearch.G.find(hsearch.model.board(cell1.i)(cell1.j)).get, hsearch.G.find(hsearch.model.board(cell2.i)(cell2.j)).get)) = Set(hsearch.getStrongCarriers(hsearch.model.board(cell1.i)(cell1.j), hsearch.model.board(cell2.i)(cell2.j), true) - hsearch.model.board(cell.i)(cell.j))
          hsearch.C((hsearch.G.find(hsearch.model.board(cell1.i)(cell1.j)).get, hsearch.G.find(hsearch.model.board(cell2.i)(cell2.j)).get)) = Set()
        }
      }
    }


    return hsearch
  }

  def clone(X: collection.mutable.Map[(Cell, Cell), Set[Set[Cell]]], h : HSearch) : collection.mutable.Map[(Cell, Cell), Set[Set[Cell]]] = {
    val newX = collection.mutable.Map[(Cell, Cell), Set[Set[Cell]]]()
    for((c1, c2) <- X.keys){

      var cell1 = getCell(c1.i, c1.j, h)
      var cell2 = getCell(c2.i, c2.j, h)


      if((cell1.colour == colour || cell1.colour == O) && (cell2.colour == colour || cell2.colour == O)){
        var masterSet : Set[Set[Cell]] = Set()
        for(set <- X((G.find(c1).get, G.find(c2).get))) {
          var subset: Set[Cell] = Set()
          for(cell <- set){
            subset = subset + getCell(cell.i, cell.j, h)
          }
          masterSet = masterSet + subset
        }
        newX((h.G.find(cell1).get, h.G.find(cell2).get)) = masterSet
      }




    }
    return newX
  }

  def getCell(i : Int, j : Int, h : HSearch) : Cell = {
    var cell = new Cell(-1,-1)
    if(i < h.model.N && i >= 0 && j < h.model.N && j >= 0){
      cell = h.model.board(i)(j)
    }
    else if(i == -1){
      cell = HSearch.boundaryBlue1
    }
    else if(i == -2){
      cell = HSearch.boundaryBlue2
    }
    else if(j == -1){
      cell = HSearch.boundaryRed1
    }
    else if(j == -2){
      cell = HSearch.boundaryRed2
    }
    return cell
  }
  def search : Unit = {

    var currentNewVC = false
    var previousNewVC = true



    while (currentNewVC || previousNewVC) {

      val tempC = C
      previousNewVC = currentNewVC
      currentNewVC = false
      for (g <- G.getReps) {


        for (g1 <- G.getReps; g2 <- G.getReps) {


          if (g1 != g2 && (newCarrier(oldC, g1, g) || newCarrier(oldC, g2, g)) && (!(g.colour == colour) || (g1.colour == O && g2.colour == O))) {

            for (c1 <- C((g1, g)); c2 <- C((g2, g))) {

              if ((!oldC((g1, g)).contains(c1) || !oldC((g2, g)).contains(c2)) && (c1 & c2).isEmpty && !c2.contains(g1) && !c1.contains(g2)) {

                currentNewVC = true
                if (g.colour == colour && (c1.size + c2.size) <= HSearch.M) {
                  C((g1, g2)) = C((g1, g2)) + (c1 ++ c2)

                }
                else {
                  val sc = c1 ++ Set(g) ++ c2
                  SC((g1, g2)) = SC((g1, g2)) + sc

                  C((g1, g2)) = apply(C((g1, g2)), (SC((g1, g2)) - sc).take(HSearch.X), sc, sc)
                }
              }
            }
          }
        }
      }
      oldC = tempC

    }
  }

  def getStrongCarriers(cell1 : Cell, cell2 : Cell, getAll : Boolean) :Set[Cell] = {

    var minSet : Set[Cell] = Set()
    var minSize = Int.MaxValue


    if(!((cell1.colour.equals(colour) && cell2.colour.equals(colour)) || (cell1.colour.equals(colour) && cell2.colour.equals(O)) || (cell2.colour.equals(colour) && cell1.colour.equals(O)) || (cell1.colour.equals(O) && cell2.colour.equals(O)))){
      return Set()
    }
    if(getAll){
      for(set <- C((G.find(cell1).get, G.find(cell2).get))){

        if(minSize > set.size){
          minSet = set
          minSize = set.size
        }
      }
      return minSet
    }
    else{
      if(cell1.colour.equals(colour) || cell2.colour.equals(colour)){
        for(set <- C((G.find(cell1).get, G.find(cell2).get))){

          if(minSize > set.size){
            minSet = set
            minSize = set.size
          }
        }
      }
    }
    return Set()
  }
  def getWeakCarriers(cell1 : Cell, cell2 : Cell, getAll : Boolean) :Set[Cell] = {

    var minSet : Set[Cell] = Set()
    var minSize = Int.MaxValue


    if(!((cell1.colour.equals(colour) && cell2.colour.equals(colour)) || (cell1.colour.equals(colour) && cell2.colour.equals(O)) || (cell2.colour.equals(colour) && cell1.colour.equals(O)) || (cell1.colour.equals(O) && cell2.colour.equals(O)))){
      return Set()
    }
    if(getAll){
      for(set <- SC((G.find(cell1).get, G.find(cell2).get))){

        if(minSize > set.size){
          minSet = set
          minSize = set.size
        }
      }
      return minSet
    }
    else{
      if(cell1.colour.equals(colour) || cell2.colour.equals(colour)){
        for(set <- SC((G.find(cell1).get, G.find(cell2).get))){

          if(minSize > set.size){
            minSet = set
            minSize = set.size
          }
        }
      }
    }
    return Set()
  }
  def newCarrier(oldC: collection.mutable.Map[(Cell, Cell), Set[Set[Cell]]], x : Cell, y : Cell) : Boolean = {

    val a = try{
      oldC((x, y)).size
    }
    catch{
      case _ : Exception => 0
    }
    val b = try{
      C((x, y)).size
    }
    catch{
      case _ : Exception => 0
    }

    return a!=b


  }
  def apply(C_set : Set[Set[Cell]], SC_set : Set[Set[Cell]], union : Set[Cell], intersection: Set[Cell]) : Set[Set[Cell]] = {
    var C_clone = C_set

    for(scl <- SC_set){

      val ul = scl ++ union
      val il = scl & intersection
      if(il.isEmpty && ul.size <= HSearch.M){
        C_clone += ul
      }
      else{

        C_clone = apply(C_clone, SC_set - scl, ul, il)
      }
    }
    return C_clone
  }





  def areNearestNeighbours(g1 : Cell, g2: Cell): Boolean ={
    val x1 = g1.i
    val x2 = g2.i
    val y1 = g1.j
    val y2 = g2.j

    var h = (x1 == x2 && y1 == y2+1) || (x1 == x2 && y1 == y2-1) || (y1 == y2 && x1 == x2+1) || (y1 == y2 && x1 == x2-1) || (x1 == x2+1 && y1 == y2+1) || (x1 == x2-1 && y1 == y2-1)
    h = h || (x1 == 0 && x2 == -1) || (x1 == model.N-1 && x2 == -2) || (y1 == 0 && y2 == -1) || (y1 == model.N-1 && y2 == -2)
    h = h || (x2 == 0 && x1 == -1) || (x2 == model.N-1 && x1 == -2) || (y2 == 0 && y1 == -1) || (y2 == model.N-1 && y1 == -2)
    return h
  }

  private def result(mod: Model, cell: Cell, col: Colour): Model = {
    val mod2 = mod.copy()
    mod2.playMove(cell, col)
    return mod2

  }


}

object HSearch extends Const{
  val M = 4
  val X = 4
  val boundaryRed1 : Cell = new Cell(0, -1)
  val boundaryRed2 : Cell = new Cell(0, -2)
  val boundaryBlue1 : Cell = new Cell(-1, 0)
  val boundaryBlue2 : Cell = new Cell(-2, 0)
  boundaryRed1.colour = R
  boundaryRed2.colour = R
  boundaryBlue1.colour = B
  boundaryBlue2.colour = B
}

