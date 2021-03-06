class RotateExtension < DanceExtension
  def initialize
    super "Rotate", "Rotates the sprite by an amount", "DeflatedPickle"

    @original = FAOSDance.getZRotation
    @max = 360
    @increase = 8
    @counter = 0
  end

  def pre_draw(graphics)
    if @counter < @max
      @counter += @increase
    else
      @counter = 0
    end

    FAOSDance.setZRotation @counter
  end

  def settings(panel)
    increase_widgets = FAOSDanceSettings.createOptionInteger panel, "Increase:", @increase, 180, 1
    increase_widgets.third.addChangeListener {|it|
      @increase = it.source.to_java(javax::swing::JSpinner).model.value.to_java(java::lang::Float).doubleValue
    }
  end

  def enable
    @original = FAOSDance.getZRotation
  end

  def disable
    FAOSDance.setZRotation @original
  end
end

FAOSDance.registerExtension RotateExtension.new
